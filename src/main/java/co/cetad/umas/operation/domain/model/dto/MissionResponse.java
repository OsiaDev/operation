package co.cetad.umas.operation.domain.model.dto;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de response para misiones de drones
 */
public record MissionResponse(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("name")
        String name,

        @JsonProperty("droneId")
        UUID droneId,

        @JsonProperty("routeId")
        UUID routeId,

        @JsonProperty("operatorId")
        UUID operatorId,

        @JsonProperty("missionType")
        MissionOrigin missionType,

        @JsonProperty("state")
        MissionState state,

        @JsonProperty("startDate")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime startDate,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @JsonProperty("updatedAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,

        @JsonProperty("hasRoute")
        boolean hasRoute,

        @JsonProperty("hasName")
        boolean hasName,

        @JsonProperty("isScheduledForFuture")
        boolean isScheduledForFuture,

        @JsonProperty("isManual")
        boolean isManual,

        @JsonProperty("isPendingApproval")
        boolean isPendingApproval
) {

    public static MissionResponse from(
            UUID id,
            String name,
            UUID droneId,
            UUID routeId,
            UUID operatorId,
            MissionOrigin missionType,
            MissionState state,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean hasRoute,
            boolean hasName,
            boolean isScheduledForFuture,
            boolean isManual,
            boolean isPendingApproval
    ) {
        return new MissionResponse(
                id, name, droneId, routeId, operatorId,
                missionType, state, startDate, createdAt, updatedAt,
                hasRoute, hasName, isScheduledForFuture, isManual, isPendingApproval
        );
    }

}