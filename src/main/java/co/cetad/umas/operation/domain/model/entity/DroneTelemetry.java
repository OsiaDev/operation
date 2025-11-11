package co.cetad.umas.operation.domain.model.entity;

import co.cetad.umas.operation.domain.model.vo.GeoLocation;
import co.cetad.umas.operation.domain.model.vo.TelemetryMetrics;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Entidad que representa un registro de telemetría del dron
 */
public record DroneTelemetry(
        String id,
        String vehicleId,
        TelemetryType type,
        GeoLocation location,
        TelemetryMetrics metrics,
        LocalDateTime timestamp,
        LocalDateTime createdAt
) {
    public static DroneTelemetry create(
            String vehicleId,
            GeoLocation location,
            TelemetryMetrics metrics
    ) {
        return new DroneTelemetry(
                UUID.randomUUID().toString(),
                vehicleId,
                TelemetryType.FULL_TELEMETRY,
                location,
                metrics,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public DroneTelemetry withType(TelemetryType newType) {
        return new DroneTelemetry(
                id, vehicleId, newType, location, metrics, timestamp, createdAt
        );
    }

    public boolean isLocationOnly() {
        return type == TelemetryType.LOCATION;
    }

    public String toMqttPayload() {
        return String.format(Locale.US, """
            {
                "vehicleId": "%s",
                "lat": %.6f,
                "lng": %.6f,
                "alt": %.2f,
                "heading": %.2f,
                "speed": %.2f,
                "accuracy": %.2f,
                "battery": %.1f,
                "timestamp": "%s"
            }
            """,
                vehicleId,
                location.latitude(),
                location.longitude(),
                location.altitude() != null ? location.altitude() : 0.0,
                metrics.heading() != null ? metrics.heading() : 0.0,
                metrics.speed() != null ? metrics.speed() : 0.0,
                location.accuracy() != null ? location.accuracy() : 0.0,
                metrics.batteryLevel() != null ? metrics.batteryLevel() : 0.0,
                timestamp.toString()
        );
    }

}