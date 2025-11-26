package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.dto.ExecutionCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para enviar comandos de ejecución de misiones
 * Define las operaciones sin acoplar al mecanismo de mensajería
 */
public interface CommandExecutionPublisher {

    /**
     * Publica un comando de ejecución de misión al sistema de mensajería
     *
     * @param command Comando de ejecución con waypoints de la ruta
     * @return CompletableFuture que se completa cuando el mensaje se envía
     */
    CompletableFuture<Void> publishExecutionCommand(ExecutionCommand command);

}