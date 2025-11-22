package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de request para aprobar misiones de drones
 */
public record ApproveMissionRequest(
        @JsonProperty("commanderName")
        @NotBlank(message = "Commander name is required")
        String commanderName
) {

    public ApproveMissionRequest {
        // Validar y normalizar commanderName
        if (commanderName != null) {
            commanderName = commanderName.trim();
            if (commanderName.isEmpty()) {
                throw new IllegalArgumentException("Commander name cannot be empty");
            }
        }
    }

}