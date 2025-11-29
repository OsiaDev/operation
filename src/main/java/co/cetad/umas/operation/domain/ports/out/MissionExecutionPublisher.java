package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.dto.MissionExecutionCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para enviar comandos de ejecución de misiones
 * Define las operaciones sin acoplar al mecanismo de mensajería
 *
 * REFACTORIZACIÓN: Ahora publica UN SOLO mensaje con todos los drones
 * y sus respectivos waypoints
 */
public interface MissionExecutionPublisher {

    /**
     * Publica un comando de ejecución de misión al sistema de mensajería
     * El comando contiene todos los drones asignados a la misión con sus waypoints
     *
     * @param command Comando de ejecución con lista de drones y sus waypoints
     * @return CompletableFuture que se completa cuando el mensaje se envía
     */
    CompletableFuture<Void> publishExecutionCommand(MissionExecutionCommand command);

}