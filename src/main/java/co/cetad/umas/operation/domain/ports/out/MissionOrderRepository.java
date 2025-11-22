package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.MissionOrder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de órdenes de misión
 */
public interface MissionOrderRepository {

    /**
     * Guarda una orden de misión
     */
    CompletableFuture<MissionOrder> save(MissionOrder order);

    /**
     * Busca una orden por ID
     */
    CompletableFuture<Optional<MissionOrder>> findById(String id);

}