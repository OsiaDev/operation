package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para crear misiones (CQRS - Command Side)
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission y DroneMissionAssignment separados
 */
public interface CreateMissionUseCase {

    /**
     * Crea una nueva misión manual con drones asignados y su orden asociada
     *
     * @param mission Misión a crear
     * @param commanderName Nombre del comandante que crea la misión
     * @param droneAssignments Lista de asignaciones de drones a la misión
     * @return Misión creada
     */
    CompletableFuture<Mission> createMission(
            Mission mission,
            String commanderName,
            List<DroneMissionAssignment> droneAssignments
    );

}