package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

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
        // Validar y normalizar droneId
        if (droneId != null) {
            droneId = droneId.trim();
            if (droneId.isEmpty()) {
                throw new IllegalArgumentException("Drone ID cannot be empty");
            }
            validateUUID(droneId, "Drone ID");
        }

        // Validar y normalizar operatorId
        if (operatorId != null) {
            operatorId = operatorId.trim();
            if (operatorId.isEmpty()) {
                throw new IllegalArgumentException("Operator ID cannot be empty");
            }
            validateUUID(operatorId, "Operator ID");
        }

        // Validar commanderName
        if (commanderName != null) {
            commanderName = commanderName.trim();
            if (commanderName.isEmpty()) {
                throw new IllegalArgumentException("Commander name cannot be empty");
            }
        }

        // Validar y normalizar routeId (opcional)
        if (routeId != null) {
            routeId = routeId.trim();
            if (routeId.isEmpty()) {
                routeId = null;
            } else {
                validateUUID(routeId, "Route ID");
            }
        }

        // Normalizar name (opcional)
        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                name = null;
            }
        }
    }

    /**
     * Valida que un String sea un UUID válido
     */
    private static void validateUUID(String value, String fieldName) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("%s must be a valid UUID: %s", fieldName, value),
                    e
            );
        }
    }

    /**
     * Convierte droneId a UUID
     */
    public UUID getDroneIdAsUUID() {
        return UUID.fromString(droneId);
    }

    /**
     * Convierte routeId a UUID (puede ser null)
     */
    public UUID getRouteIdAsUUID() {
        return routeId != null ? UUID.fromString(routeId) : null;
    }

    /**
     * Convierte operatorId a UUID
     */
    public UUID getOperatorIdAsUUID() {
        return UUID.fromString(operatorId);
    }

}