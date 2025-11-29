package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para ejecutar misiones (CQRS - Command Side)
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission
 */
public interface ExecuteMissionUseCase {

    /**
     * Autoriza y ejecuta una misión cambiando su estado de APROBADA a EN_EJECUCION
     * Busca todos los drones asignados y publica comandos de ejecución para cada uno
     *
     * @param missionId ID de la misión a ejecutar
     * @param commanderName Nombre del comandante que autoriza la ejecución
     * @return Misión en ejecución con estado actualizado
     */
    CompletableFuture<Mission> executeMission(String missionId, String commanderName);

}