package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de request para ejecutar comandos de drones
 * Opcionalmente puede incluir el nombre del comandante para auditoría
 */
public record CommandRequest(
        @JsonProperty("commanderName")
        String commanderName
) {

    public CommandRequest {
        // Normalizar commanderName (opcional)
        if (commanderName != null) {
            commanderName = commanderName.trim();
            if (commanderName.isEmpty()) {
                commanderName = null;
            }
        }
    }

}