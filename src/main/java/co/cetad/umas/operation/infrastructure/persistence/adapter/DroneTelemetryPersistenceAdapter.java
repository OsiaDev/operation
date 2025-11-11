package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.ports.out.DroneTelemetryRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneTelemetryMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia que implementa el puerto de salida
 * Convierte entre operaciones reactivas (Reactor) y CompletableFuture
 * Mantiene el desacoplamiento entre dominio e infraestructura
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneTelemetryPersistenceAdapter implements DroneTelemetryRepository {

    private final R2dbcDroneTelemetryRepository r2dbcRepository;

    @Override
    public CompletableFuture<DroneTelemetry> save(DroneTelemetry telemetry) {
        return toCompletableFuture(
                Mono.just(telemetry)
                        .map(DroneTelemetryMapper.toEntity)
                        .flatMap(r2dbcRepository::save)
                        .map(DroneTelemetryMapper.toDomain)
                        .doOnSuccess(saved -> log.debug("Saved telemetry: {} for vehicle: {}",
                                saved.id(), saved.vehicleId()))
                        .doOnError(error -> log.error("Error saving telemetry for vehicle: {}",
                                telemetry.vehicleId(), error))
        );
    }

    @Override
    public CompletableFuture<Optional<DroneTelemetry>> findById(String id) {
        return toCompletableFuture(
                r2dbcRepository.findById(id)
                        .map(DroneTelemetryMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .doOnSuccess(result -> {
                            if (result.isPresent()) {
                                log.debug("Found telemetry by id: {}", id);
                            } else {
                                log.debug("Telemetry not found by id: {}", id);
                            }
                        })
        );
    }

    @Override
    public CompletableFuture<Optional<DroneTelemetry>> findLatestByVehicleId(String vehicleId) {
        return toCompletableFuture(
                r2dbcRepository.findLatestByVehicleId(vehicleId)
                        .map(DroneTelemetryMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .doOnSuccess(result -> log.debug("Found latest telemetry for vehicle: {} - Present: {}",
                                vehicleId, result.isPresent()))
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
                        .map(DroneTelemetryMapper.toDomain)
                        .collectList()
                        .doOnSuccess(list -> log.debug("Found {} telemetry records for vehicle: {} in date range",
                                list.size(), vehicleId))
        );
    }

    @Override
    public CompletableFuture<List<DroneTelemetry>> findRecentByVehicleId(
            String vehicleId,
            int limit
    ) {
        return toCompletableFuture(
                r2dbcRepository.findRecentByVehicleId(vehicleId, limit)
                        .map(DroneTelemetryMapper.toDomain)
                        .collectList()
                        .doOnSuccess(list -> log.debug("Found {} recent telemetry records for vehicle: {}",
                                list.size(), vehicleId))
        );
    }

    @Override
    public CompletableFuture<Long> deleteOlderThan(LocalDateTime date) {
        return toCompletableFuture(
                r2dbcRepository.deleteOlderThan(date)
                        .doOnSuccess(count -> log.info("Deleted {} telemetry records older than {}",
                                count, date))
        );
    }

    /**
     * Convierte un Mono a CompletableFuture de forma limpia
     * Maneja errores y propaga excepciones correctamente
     */
    private <T> CompletableFuture<T> toCompletableFuture(Mono<T> mono) {
        return mono
                .toFuture()
                .exceptionally(throwable -> {
                    log.error("Error in reactive operation", throwable);
                    throw new RuntimeException("Database operation failed", throwable);
                });
    }

}