package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.ports.in.TelemetryQueryUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de consultas de telemetría (CQRS - Query Side)
 *
 * Responsabilidad única:
 * - Coordinar consultas de telemetría
 * - Aplicar lógica de validación de parámetros
 * - Delegar al repository
 *
 * Sin efectos secundarios, solo lectura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryQueryService implements TelemetryQueryUseCase {

    private final DroneTelemetryRepository telemetryRepository;

    @Override
    public CompletableFuture<Optional<DroneTelemetry>> findById(String id) {
        log.debug("Querying telemetry by id: {}", id);

        return validateId(id)
                .thenCompose(validId -> telemetryRepository.findById(validId))
                .exceptionally(throwable -> {
                    log.error("Error querying telemetry by id: {}", id, throwable);
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<Optional<DroneTelemetry>> findLatestByVehicleId(String vehicleId) {
        log.debug("Querying latest telemetry for vehicle: {}", vehicleId);

        return validateVehicleId(vehicleId)
                .thenCompose(validVehicleId -> telemetryRepository.findLatestByVehicleId(validVehicleId))
                .exceptionally(throwable -> {
                    log.error("Error querying latest telemetry for vehicle: {}", vehicleId, throwable);
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<List<DroneTelemetry>> findByVehicleIdAndDateRange(
            String vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        log.debug("Querying telemetry for vehicle: {} between {} and {}",
                vehicleId, startDate, endDate);

        return validateDateRangeQuery(vehicleId, startDate, endDate)
                .thenCompose(params -> telemetryRepository.findByVehicleIdAndDateRange(
                        params.vehicleId(),
                        params.startDate(),
                        params.endDate()
                ))
                .exceptionally(throwable -> {
                    log.error("Error querying telemetry for vehicle: {} in date range",
                            vehicleId, throwable);
                    return List.of();
                });
    }

    @Override
    public CompletableFuture<List<DroneTelemetry>> findRecentByVehicleId(
            String vehicleId,
            int limit
    ) {
        log.debug("Querying recent telemetry for vehicle: {} with limit: {}", vehicleId, limit);

        return validateRecentQuery(vehicleId, limit)
                .thenCompose(params -> telemetryRepository.findRecentByVehicleId(
                        params.vehicleId(),
                        params.limit()
                ))
                .exceptionally(throwable -> {
                    log.error("Error querying recent telemetry for vehicle: {}", vehicleId, throwable);
                    return List.of();
                });
    }

    /**
     * Valida el ID
     */
    private CompletableFuture<String> validateId(String id) {
        return CompletableFuture.supplyAsync(() -> {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID cannot be null or empty");
            }
            return id;
        });
    }

    /**
     * Valida el vehicleId
     */
    private CompletableFuture<String> validateVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            if (vehicleId == null || vehicleId.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID cannot be null or empty");
            }
            return vehicleId;
        });
    }

    /**
     * Valida parámetros de consulta por rango de fechas
     */
    private CompletableFuture<DateRangeQueryParams> validateDateRangeQuery(
            String vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (vehicleId == null || vehicleId.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID cannot be null or empty");
            }
            if (startDate == null) {
                throw new IllegalArgumentException("Start date cannot be null");
            }
            if (endDate == null) {
                throw new IllegalArgumentException("End date cannot be null");
            }
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }

            // Validar rango máximo (ej: 90 días)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 90) {
                throw new IllegalArgumentException("Date range cannot exceed 90 days");
            }

            return new DateRangeQueryParams(vehicleId, startDate, endDate);
        });
    }

    /**
     * Valida parámetros de consulta reciente
     */
    private CompletableFuture<RecentQueryParams> validateRecentQuery(
            String vehicleId,
            int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (vehicleId == null || vehicleId.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID cannot be null or empty");
            }
            if (limit <= 0) {
                throw new IllegalArgumentException("Limit must be greater than 0");
            }
            if (limit > 1000) {
                throw new IllegalArgumentException("Limit cannot exceed 1000");
            }

            return new RecentQueryParams(vehicleId, limit);
        });
    }

    /**
     * Records para encapsular parámetros validados
     */
    private record DateRangeQueryParams(
            String vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {}

    private record RecentQueryParams(
            String vehicleId,
            int limit
    ) {}

}