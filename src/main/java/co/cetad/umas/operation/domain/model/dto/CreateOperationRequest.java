package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO de request para crear operaciones de drones
 */
public record CreateOperationRequest(
        @JsonProperty("droneId")
        @NotBlank(message = "Drone ID is required")
        String droneId,

        @JsonProperty("routeId")
        String routeId,

        @JsonProperty("startDate")
        @NotNull(message = "Start date is required")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate
) {

    public CreateOperationRequest {
        if (droneId != null) {
            droneId = droneId.trim();
            if (droneId.isEmpty()) {
                throw new IllegalArgumentException("Drone ID cannot be empty");
            }
        }

        if (routeId != null) {
            routeId = routeId.trim();
            if (routeId.isEmpty()) {
                routeId = null;
            }
        }
    }

}