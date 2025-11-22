package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para ejecutar misiones de drones (CQRS - Command Side)
 */
public interface ExecuteMissionUseCase {

    /**
     * Autoriza y ejecuta una misión cambiando su estado de APROBADA a EN_EJECUCION
     *
     * @param missionId ID de la misión a ejecutar
     * @param commanderName Nombre del comandante que autoriza la ejecución
     * @return Misión en ejecución con estado actualizado
     */
    CompletableFuture<DroneMission> executeMission(String missionId, String commanderName);

}