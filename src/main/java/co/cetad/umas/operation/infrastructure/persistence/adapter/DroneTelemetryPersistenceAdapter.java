package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.ports.out.DroneTelemetryRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneTelemetryMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Adaptador de persistencia mejorado con mejor manejo de errores y timeouts
 *
 * Mejoras:
 * - Timeouts configurables para operaciones DB
 * - Manejo de errores más robusto
 * - Logging detallado
 * - Retry automático en fallos transitorios
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneTelemetryPersistenceAdapter implements DroneTelemetryRepository {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 2;

    private final R2dbcDroneTelemetryRepository r2dbcRepository;

    @Override
    public CompletableFuture<DroneTelemetry> save(DroneTelemetry telemetry) {
        return toCompletableFuture(
                Mono.just(telemetry)
                        .map(DroneTelemetryMapper.toEntity)
                        .flatMap(r2dbcRepository::save)
                        .retryWhen(retrySpec())
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneTelemetryMapper.toDomain)
                        .doOnSuccess(saved ->
                                log.debug("✅ Saved telemetry: {} for vehicle: {} at {}",
                                        saved.id(), saved.vehicleId(), saved.timestamp()))
                        .doOnError(error ->
                                log.error("❌ Error saving telemetry for vehicle: {}",
                                        telemetry.vehicleId(), error)),
                () -> "Failed to save telemetry for vehicle: " + telemetry.vehicleId()
        );
    }

    @Override
    public CompletableFuture<Optional<DroneTelemetry>> findById(String id) {
        return toCompletableFuture(
                r2dbcRepository.findById(id)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneTelemetryMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .doOnSuccess(result -> {
                            if (result.isPresent()) {
                                log.debug("Found telemetry by id: {}", id);
                            } else {
                                log.debug("Telemetry not found by id: {}", id);
                            }
                        }),
                () -> "Failed to find telemetry by id: " + id
        );
    }

    @Override
    public CompletableFuture<Optional<DroneTelemetry>> findLatestByVehicleId(String vehicleId) {
        return toCompletableFuture(
                r2dbcRepository.findLatestByVehicleId(vehicleId)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneTelemetryMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .doOnSuccess(result ->
                                log.debug("Found latest telemetry for vehicle: {} - Present: {}",
                                        vehicleId, result.isPresent())),
                () -> "Failed to find latest telemetry for vehicle: " + vehicleId
        );
    }

    @Override
    public CompletableFuture<List<DroneTelemetry>> findByVehicleIdAndDateRange(
            String vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return toCompletableFuture(
                r2dbcRepository.findByVehicleIdAndDateRange(vehicleId, startDate, endDate)
                        .timeout(Duration.ofSeconds(10))
                        .map(DroneTelemetryMapper.toDomain)
                        .collectList()
                        .doOnSuccess(list ->
                                log.debug("Found {} telemetry records for vehicle: {} in range [{} - {}]",
                                        list.size(), vehicleId, startDate, endDate)),
                () -> String.format("Failed to find telemetry for vehicle: %s in date range", vehicleId)
        );
    }

    @Override
    public CompletableFuture<List<DroneTelemetry>> findRecentByVehicleId(
            String vehicleId,
            int limit
    ) {
        return toCompletableFuture(
                r2dbcRepository.findRecentByVehicleId(vehicleId, limit)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneTelemetryMapper.toDomain)
                        .collectList()
                        .doOnSuccess(list ->
                                log.debug("Found {} recent telemetry records for vehicle: {}",
                                        list.size(), vehicleId)),
                () -> "Failed to find recent telemetry for vehicle: " + vehicleId
        );
    }

    @Override
    public CompletableFuture<Long> deleteOlderThan(LocalDateTime date) {
        return toCompletableFuture(
                r2dbcRepository.deleteOlderThan(date)
                        .timeout(Duration.ofSeconds(30))
                        .doOnSuccess(count ->
                                log.info("Deleted {} telemetry records older than {}", count, date)),
                () -> "Failed to delete old telemetry records"
        );
    }

    /**
     * Convierte un Mono a CompletableFuture con manejo robusto de errores
     *
     * @param mono El Mono reactivo a convertir
     * @param errorMessageSupplier Proveedor del mensaje de error
     * @return CompletableFuture con resultado o excepción
     */
    private <T> CompletableFuture<T> toCompletableFuture(
            Mono<T> mono,
            ErrorMessageSupplier errorMessageSupplier
    ) {
        return mono
                .toFuture()
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        String errorMessage = errorMessageSupplier.get();
                        log.error(errorMessage, throwable);

                        // Lanzar excepción específica según el tipo
                        if (throwable instanceof java.util.concurrent.TimeoutException) {
                            throw new DatabaseTimeoutException(errorMessage, throwable);
                        } else {
                            throw new DatabaseOperationException(errorMessage, throwable);
                        }
                    }
                    return result;
                });
    }

    /**
     * Configuración de retry para operaciones transitorias
     */
    private reactor.util.retry.Retry retrySpec() {
        return reactor.util.retry.Retry
                .backoff(MAX_RETRIES, Duration.ofMillis(100))
                .filter(this::isRetriableError)
                .doBeforeRetry(signal ->
                        log.warn("Retrying database operation [attempt={}]: {}",
                                signal.totalRetries() + 1, signal.failure().getMessage()));
    }

    /**
     * Determina si un error es retriable
     */
    private boolean isRetriableError(Throwable throwable) {
        // Reintentar solo en errores transitorios de conexión
        String message = throwable.getMessage();
        return message != null && (
                message.contains("Connection refused") ||
                        message.contains("Connection reset") ||
                        message.contains("Temporary failure")
        );
    }

    /**
     * Interfaz funcional para mensajes de error
     */
    @FunctionalInterface
    private interface ErrorMessageSupplier {
        String get();
    }

    /**
     * Excepción personalizada para timeouts de BD
     */
    public static class DatabaseTimeoutException extends RuntimeException {
        public DatabaseTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción personalizada para operaciones de BD
     */
    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}