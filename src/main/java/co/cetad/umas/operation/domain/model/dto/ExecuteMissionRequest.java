package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de request para ejecutar misiones de drones
 * Autoriza y pone en ejecución una misión aprobada
 */
public record ExecuteMissionRequest(
        @JsonProperty("commanderName")
        @NotBlank(message = "Commander name is required")
        String commanderName
) {

    public ExecuteMissionRequest {
        // Validar y normalizar commanderName
        if (commanderName != null) {
            commanderName = commanderName.trim();
            if (commanderName.isEmpty()) {
                throw new IllegalArgumentException("Commander name cannot be empty");
            }
        }
    }

}