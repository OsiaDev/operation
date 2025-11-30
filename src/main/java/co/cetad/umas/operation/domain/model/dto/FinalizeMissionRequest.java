package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de request para finalizar misiones de drones
 * Cierra una misión en ejecución y crea registro de auditoría
 */
public record FinalizeMissionRequest(
        @JsonProperty("commanderName")
        @NotBlank(message = "Commander name is required")
        String commanderName
) {

    public FinalizeMissionRequest {
        // Validar y normalizar commanderName
        if (commanderName != null) {
            commanderName = commanderName.trim();
            if (commanderName.isEmpty()) {
                throw new IllegalArgumentException("Commander name cannot be empty");
            }
        }
    }

}