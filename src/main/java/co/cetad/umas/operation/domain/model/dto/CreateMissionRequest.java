package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de request para crear misiones con drones asignados
 *
 * REFACTORIZACIÓN: Ahora soporta múltiples drones asignados a una misión
 * Cada dron puede tener su propia ruta
 */
public record CreateMissionRequest(
        @JsonProperty("name")
        String name,

        @JsonProperty("operatorId")
        @NotBlank(message = "Operator ID is required")
        String operatorId,

        @JsonProperty("commanderName")
        @NotBlank(message = "Commander name is required")
        String commanderName,

        @JsonProperty("estimatedDate")
        @NotNull(message = "Estimated date is required")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime estimatedDate,

        @JsonProperty("droneAssignments")
        @NotEmpty(message = "At least one drone must be assigned")
        List<DroneAssignmentRequest> droneAssignments
) {

    public CreateMissionRequest {
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

        // Normalizar name (opcional)
        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                name = null;
            }
        }

        // Validar droneAssignments
        if (droneAssignments != null && !droneAssignments.isEmpty()) {
            // Verificar que no haya drones duplicados
            long uniqueDrones = droneAssignments.stream()
                    .map(DroneAssignmentRequest::droneId)
                    .distinct()
                    .count();

            if (uniqueDrones != droneAssignments.size()) {
                throw new IllegalArgumentException(
                        "Duplicate drone IDs found in assignments. Each drone can only be assigned once to a mission.");
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
     * Convierte operatorId a UUID
     */
    public UUID getOperatorIdAsUUID() {
        return UUID.fromString(operatorId);
    }

    /**
     * DTO para asignación de dron a misión
     */
    public record DroneAssignmentRequest(
            @JsonProperty("droneId")
            @NotBlank(message = "Drone ID is required")
            String droneId,

            @JsonProperty("routeId")
            String routeId
    ) {
        public DroneAssignmentRequest {
            // Validar y normalizar droneId
            if (droneId != null) {
                droneId = droneId.trim();
                if (droneId.isEmpty()) {
                    throw new IllegalArgumentException("Drone ID cannot be empty");
                }
                validateUUID(droneId, "Drone ID");
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
        }

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

        public UUID getDroneIdAsUUID() {
            return UUID.fromString(droneId);
        }

        public UUID getRouteIdAsUUID() {
            return routeId != null ? UUID.fromString(routeId) : null;
        }
    }

}