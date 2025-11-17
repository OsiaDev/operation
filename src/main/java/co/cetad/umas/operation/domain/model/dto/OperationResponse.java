package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO de response para operaciones de drones
 */
public record OperationResponse(
        @JsonProperty("id")
        String id,

        @JsonProperty("droneId")
        String droneId,

        @JsonProperty("routeId")
        String routeId,

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

        @JsonProperty("isScheduledForFuture")
        boolean isScheduledForFuture
) {

    public static OperationResponse from(
            String id,
            String droneId,
            String routeId,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean hasRoute,
            boolean isScheduledForFuture
    ) {
        return new OperationResponse(
                id, droneId, routeId, startDate,
                createdAt, updatedAt, hasRoute, isScheduledForFuture
        );
    }

}