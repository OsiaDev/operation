package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO de request para crear misiones de drones
 */
public record CreateMissionRequest(
        @JsonProperty("name")
        String name,

        @JsonProperty("droneId")
        @NotBlank(message = "Drone ID is required")
        String droneId,

        @JsonProperty("routeId")
        String routeId,

        @JsonProperty("operatorId")
        @NotBlank(message = "Operator ID is required")
        String operatorId,

        @JsonProperty("commanderName")
        @NotBlank(message = "Commander name is required")
        String commanderName,

        @JsonProperty("startDate")
        @NotNull(message = "Start date is required")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate
) {

    public CreateMissionRequest {
        if (droneId != null) {
            droneId = droneId.trim();
            if (droneId.isEmpty()) {
                throw new IllegalArgumentException("Drone ID cannot be empty");
            }
        }

        if (operatorId != null) {
            operatorId = operatorId.trim();
            if (operatorId.isEmpty()) {
                throw new IllegalArgumentException("Operator ID cannot be empty");
            }
        }

        if (commanderName != null) {
            commanderName = commanderName.trim();
            if (commanderName.isEmpty()) {
                throw new IllegalArgumentException("Commander name cannot be empty");
            }
        }

        if (routeId != null) {
            routeId = routeId.trim();
            if (routeId.isEmpty()) {
                routeId = null;
            }
        }

        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                name = null;
            }
        }
    }

}