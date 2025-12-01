package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.MissionExecution;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de ejecuciones de misión
 */
public interface MissionExecutionRepository {

    /**
     * Guarda una ejecución de misión
     */
    CompletableFuture<MissionExecution> save(MissionExecution execution);

    /**
     * Busca una ejecución por ID
     */
    CompletableFuture<Optional<MissionExecution>> findById(String id);

    /**
     * Busca una ejecución por ID de misión
     * Útil para obtener quién ejecutó una misión específica
     */
    CompletableFuture<Optional<MissionExecution>> findByMissionId(String missionId);

}