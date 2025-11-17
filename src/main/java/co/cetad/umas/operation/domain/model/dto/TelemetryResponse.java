package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para consultas de telemetría
 * Optimizado para serialización JSON
 */
public record TelemetryResponse(
        @JsonProperty("id")
        String id,

        @JsonProperty("vehicleId")
        String vehicleId,

        @JsonProperty("telemetryType")
        String telemetryType,

        @JsonProperty("location")
        LocationData location,

        @JsonProperty("metrics")
        MetricsData metrics,

        @JsonProperty("timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {

    public record LocationData(
            @JsonProperty("latitude")
            double latitude,

            @JsonProperty("longitude")
            double longitude,

            @JsonProperty("altitude")
            Double altitude,

            @JsonProperty("accuracy")
            Double accuracy
    ) {}

    public record MetricsData(
            @JsonProperty("speed")
            Double speed,

            @JsonProperty("heading")
            Double heading,

            @JsonProperty("batteryLevel")
            Double batteryLevel,

            @JsonProperty("temperature")
            Double temperature,

            @JsonProperty("signalStrength")
            Double signalStrength
    ) {}

}