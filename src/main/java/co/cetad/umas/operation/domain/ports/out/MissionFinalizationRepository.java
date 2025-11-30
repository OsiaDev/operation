package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.MissionFinalization;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de finalizaciones de misión
 */
public interface MissionFinalizationRepository {

    /**
     * Guarda una finalización de misión
     */
    CompletableFuture<MissionFinalization> save(MissionFinalization finalization);

    /**
     * Busca una finalización por ID
     */
    CompletableFuture<Optional<MissionFinalization>> findById(String id);

    /**
     * Busca una finalización por ID de misión
     * Útil para verificar si una misión ya fue finalizada
     */
    CompletableFuture<Optional<MissionFinalization>> findByMissionId(String missionId);

}