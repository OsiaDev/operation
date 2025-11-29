package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de asignaciones de drones a misiones
 *
 * REFACTORIZACIÓN: Nuevo repositorio para manejar relación muchos-a-muchos
 */
public interface DroneMissionAssignmentRepository {

    /**
     * Guarda una asignación de dron a misión
     */
    CompletableFuture<DroneMissionAssignment> save(DroneMissionAssignment assignment);

    /**
     * Busca una asignación por ID
     */
    CompletableFuture<Optional<DroneMissionAssignment>> findById(String id);

    /**
     * Busca todas las asignaciones de una misión específica
     * (todos los drones asignados a una misión)
     */
    CompletableFuture<List<DroneMissionAssignment>> findByMissionId(String missionId);

    /**
     * Busca todas las asignaciones de un dron específico
     * (todas las misiones en las que está asignado un dron)
     */
    CompletableFuture<List<DroneMissionAssignment>> findByDroneId(String droneId);

    /**
     * Busca una asignación específica (misión + dron)
     */
    CompletableFuture<Optional<DroneMissionAssignment>> findByMissionIdAndDroneId(
            String missionId,
            String droneId
    );

    /**
     * Verifica si un dron ya está asignado a una misión
     */
    CompletableFuture<Boolean> existsByMissionIdAndDroneId(
            String missionId,
            String droneId
    );

    /**
     * Cuenta cuántos drones están asignados a una misión
     */
    CompletableFuture<Long> countByMissionId(String missionId);

    /**
     * Busca todas las asignaciones que tienen una ruta específica
     */
    CompletableFuture<List<DroneMissionAssignment>> findByRouteId(String routeId);

    /**
     * Elimina una asignación por ID
     */
    CompletableFuture<Void> deleteById(String id);

    /**
     * Elimina todas las asignaciones de una misión
     */
    CompletableFuture<Void> deleteByMissionId(String missionId);

}