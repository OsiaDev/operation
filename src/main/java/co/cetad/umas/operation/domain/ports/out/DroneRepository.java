package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.Drone;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de drones
 * Define las operaciones sin acoplar al mecanismo de persistencia
 */
public interface DroneRepository {

    /**
     * Guarda un dron (INSERT o UPDATE)
     */
    CompletableFuture<Drone> save(Drone drone);

    /**
     * Busca un dron por ID
     */
    CompletableFuture<Optional<Drone>> findById(String id);

    /**
     * Busca un dron por vehicle ID
     * Este es el identificador que viene en la telemetría
     */
    CompletableFuture<Optional<Drone>> findByVehicleId(String vehicleId);

    /**
     * Verifica si existe un dron con el vehicle ID dado
     */
    CompletableFuture<Boolean> existsByVehicleId(String vehicleId);

}