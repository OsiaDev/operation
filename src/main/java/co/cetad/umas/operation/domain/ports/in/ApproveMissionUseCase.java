package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para aprobar misiones de drones (CQRS - Command Side)
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
    CompletableFuture<DroneMission> approveMission(String missionId, String commanderName);

}