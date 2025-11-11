package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de telemetría de drones
 * Define las operaciones sin acoplar al mecanismo de persistencia
 */
public interface DroneTelemetryRepository {

    /**
     * Guarda un registro de telemetría
     */
    CompletableFuture<DroneTelemetry> save(DroneTelemetry telemetry);

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

    /**
     * Elimina telemetría antigua (para limpieza)
     */
    CompletableFuture<Long> deleteOlderThan(LocalDateTime date);

}