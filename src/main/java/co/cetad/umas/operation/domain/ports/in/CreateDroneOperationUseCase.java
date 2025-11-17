package co.cetad.umas.operation.domain.ports.in;


import co.cetad.umas.operation.domain.model.vo.DroneOperation;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para operaciones de drones (CQRS - Command Side)
 */
public interface CreateDroneOperationUseCase {

    /**
     * Crea una nueva operación de dron
     */
    CompletableFuture<DroneOperation> createOperation(DroneOperation operation);

}