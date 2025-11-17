package co.cetad.umas.operation.domain.ports.out;


import co.cetad.umas.operation.domain.model.vo.DroneOperation;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de operaciones
 */
public interface DroneOperationRepository {

    /**
     * Guarda una operación de dron
     */
    CompletableFuture<DroneOperation> save(DroneOperation operation);

    /**
     * Busca una operación por ID
     */
    CompletableFuture<Optional<DroneOperation>> findById(String id);

}