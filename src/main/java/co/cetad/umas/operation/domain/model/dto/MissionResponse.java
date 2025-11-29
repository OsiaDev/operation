package co.cetad.umas.operation.domain.model.dto;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de response para misiones con información completa de drones asignados
 *
 * REFACTORIZACIÓN: Ahora incluye toda la información del dron, no solo IDs
 */
public record MissionResponse(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("name")
        String name,

        @JsonProperty("operatorId")
        UUID operatorId,

        @JsonProperty("missionType")
        MissionOrigin missionType,

        @JsonProperty("state")
        MissionState state,

        @JsonProperty("estimatedDate")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime estimatedDate,

        @JsonProperty("startDate")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate,

        @JsonProperty("endDate")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime endDate,

        @JsonProperty("assignedDrones")
        List<DroneAssignmentResponse> assignedDrones,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @JsonProperty("updatedAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,

        // Helpers computados
        @JsonProperty("hasName")
        boolean hasName,

        @JsonProperty("isScheduledForFuture")
        boolean isScheduledForFuture,

        @JsonProperty("isManual")
        boolean isManual,

        @JsonProperty("isPendingApproval")
        boolean isPendingApproval,

        @JsonProperty("isInProgress")
        boolean isInProgress,

        @JsonProperty("hasStarted")
        boolean hasStarted,

        @JsonProperty("hasEnded")
        boolean hasEnded,

        @JsonProperty("droneCount")
        int droneCount
) {

    /**
     * DTO para información completa de dron asignado a la misión
     * Incluye todos los atributos del dron para evitar consultas adicionales
     */
    public record DroneAssignmentResponse(
            @JsonProperty("assignmentId")
            UUID assignmentId,

            @JsonProperty("droneId")
            UUID droneId,

            @JsonProperty("droneName")
            String droneName,

            @JsonProperty("vehicleId")
            String vehicleId,

            @JsonProperty("model")
            String model,

            @JsonProperty("description")
            String description,

            @JsonProperty("serialNumber")
            String serialNumber,

            @JsonProperty("droneStatus")
            String droneStatus,

            @JsonProperty("flightHours")
            BigDecimal flightHours,

            @JsonProperty("droneCreatedAt")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime droneCreatedAt,

            @JsonProperty("droneUpdatedAt")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime droneUpdatedAt,

            @JsonProperty("routeId")
            UUID routeId,

            @JsonProperty("hasRoute")
            boolean hasRoute
    ) {}

}