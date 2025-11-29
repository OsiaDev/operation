package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para aprobar misiones (CQRS - Command Side)
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission
 */
public interface ApproveMissionUseCase {

    /**
     * Aprueba una misión cambiando su estado a APROBADA
     * y creando un registro de aprobación
     *
     * @param missionId ID de la misión a aprobar
     * @param commanderName Nombre del comandante que aprueba la misión
     * @return Misión aprobada con estado actualizado
     */
    CompletableFuture<Mission> approveMission(String missionId, String commanderName);

}