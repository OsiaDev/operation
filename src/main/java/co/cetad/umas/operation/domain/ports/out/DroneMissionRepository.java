package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de misiones de drones
 */
public interface DroneMissionRepository {

    /**
     * Guarda una misión de dron
     */
    CompletableFuture<DroneMission> save(DroneMission mission);

    /**
     * Busca una misión por ID
     */
    CompletableFuture<Optional<DroneMission>> findById(UUID id);

}