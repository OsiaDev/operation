package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para crear misiones de drones (CQRS - Command Side)
 */
public interface CreateDroneMissionUseCase {

    /**
     * Crea una nueva misión de dron manual con su orden asociada
     *
     * @param mission Misión a crear
     * @param commanderName Nombre del comandante que crea la misión
     * @return Misión creada
     */
    CompletableFuture<DroneMission> createMission(DroneMission mission, String commanderName);

}