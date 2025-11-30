package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para finalizar misiones (CQRS - Command Side)
 * Define operaciones para cerrar misiones en ejecución
 */
public interface FinalizeMissionUseCase {

    /**
     * Finaliza una misión cambiando su estado de EN_EJECUCION a FINALIZADA
     * y creando un registro de finalización
     *
     * @param missionId ID de la misión a finalizar
     * @param commanderName Nombre del comandante que finaliza la misión
     * @return Misión finalizada con estado actualizado
     */
    CompletableFuture<Mission> finalizeMission(String missionId, String commanderName);

}