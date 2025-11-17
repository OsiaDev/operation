package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para consultas de telemetría (CQRS - Query Side)
 * Define operaciones de solo lectura sin modificar el estado
 */
public interface TelemetryQueryUseCase {

    /**
     * Busca telemetría por ID
     */
    CompletableFuture<Optional<DroneTelemetry>> findById(String id);

    /**
     * Busca la última telemetría de un vehículo
     */
    CompletableFuture<Optional<DroneTelemetry>> findLatestByVehicleId(String vehicleId);

    /**
     * Busca telemetría de un vehículo en un rango de fechas
     */
    CompletableFuture<List<DroneTelemetry>> findByVehicleIdAndDateRange(
            String vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Busca telemetría reciente de un vehículo (últimas N entradas)
     */
    CompletableFuture<List<DroneTelemetry>> findRecentByVehicleId(
            String vehicleId,
            int limit
    );

}