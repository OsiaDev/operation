package co.cetad.umas.operation.domain.ports.in;

import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para ejecutar comandos de drones (CQRS - Command Side)
 * Define operaciones para enviar comandos al UGCS server
 */
public interface ExecuteCommandUseCase {

    /**
     * Ejecuta un comando específico en un dron asociado a una misión
     *
     * @param missionId ID de la misión
     * @param commandCode Código del comando UGCS a ejecutar
     * @return CompletableFuture que se completa cuando el comando se envía
     */
    CompletableFuture<Void> executeCommand(String missionId, String commandCode);

}