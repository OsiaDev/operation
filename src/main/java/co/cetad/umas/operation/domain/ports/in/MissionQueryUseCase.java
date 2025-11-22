package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para consultas de misiones (CQRS - Query Side)
 * Define operaciones de solo lectura sin modificar el estado
 */
public interface MissionQueryUseCase {

    /**
     * Busca una misión por ID
     */
    CompletableFuture<Optional<DroneMission>> findById(String id);

    /**
     * Busca todas las misiones
     */
    CompletableFuture<List<DroneMission>> findAll();

}