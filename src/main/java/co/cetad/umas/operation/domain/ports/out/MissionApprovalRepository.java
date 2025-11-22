package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.MissionApproval;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de aprobaciones de misión
 */
public interface MissionApprovalRepository {

    /**
     * Guarda una aprobación de misión
     */
    CompletableFuture<MissionApproval> save(MissionApproval approval);

    /**
     * Busca una aprobación por ID
     */
    CompletableFuture<Optional<MissionApproval>> findById(String id);

}