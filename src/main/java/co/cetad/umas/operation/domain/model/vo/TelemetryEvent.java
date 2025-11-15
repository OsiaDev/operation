package co.cetad.umas.operation.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Value Object simplificado que representa un evento de telemetría
 * Solo datos, sin lógica de negocio
 */
public record TelemetryEvent(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("altitude") double altitude,
        @JsonProperty("speed") Double speed,
        @JsonProperty("heading") Double heading,
        @JsonProperty("batteryLevel") Double batteryLevel,
        @JsonProperty("satelliteCount") Integer satelliteCount,
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("additionalFields") Map<String, Object> additionalFields
) {

    public TelemetryEvent {
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
        if (vehicleId.isBlank()) {
            throw new IllegalArgumentException("Vehicle ID cannot be empty");
        }

        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        // Limpiar additionalFields de nulls
        if (additionalFields != null) {
            additionalFields = additionalFields.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
        }
    }

    /**
     * Crea una GeoLocation a partir de las coordenadas
     */
    public GeoLocation toGeoLocation() {
        return new GeoLocation(
                latitude,
                longitude,
                altitude,
                extractAccuracy()
        );
    }

    /**
     * Crea TelemetryMetrics a partir de los campos de métricas
     */
    public TelemetryMetrics toTelemetryMetrics() {
        return new TelemetryMetrics(
                speed,
                heading,
                batteryLevel,
                extractTemperature(),
                extractSignalStrength()
        );
    }

    /**
     * Extrae temperatura desde additionalFields si existe
     */
    private Double extractTemperature() {
        if (additionalFields == null || additionalFields.isEmpty()) {
            return null;
        }
        return extractDoubleField("temperature", "temp");
    }

    /**
     * Extrae signal strength desde additionalFields si existe
     */
    private Double extractSignalStrength() {
        if (additionalFields == null || additionalFields.isEmpty()) {
            return null;
        }
        return extractDoubleField("signalStrength", "signal", "rssi");
    }

    /**
     * Extrae accuracy desde additionalFields si existe
     */
    private Double extractAccuracy() {
        if (additionalFields == null || additionalFields.isEmpty()) {
            return null;
        }
        return extractDoubleField("accuracy", "gpsAccuracy", "positionAccuracy");
    }

    /**
     * Helper para extraer campos Double con múltiples posibles nombres
     */
    private Double extractDoubleField(String... fieldNames) {
        if (additionalFields == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            Object value = additionalFields.get(fieldName);
            if (value != null) {
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                if (value instanceof String str) {
                    try {
                        return Double.parseDouble(str);
                    } catch (NumberFormatException ignored) {
                        // Continuar con el siguiente campo
                    }
                }
            }
        }
        return null;
    }

    /**
     * Factory method para testing o creación manual
     */
    public static TelemetryEvent create(
            String vehicleId,
            double latitude,
            double longitude,
            double altitude
    ) {
        return new TelemetryEvent(
                vehicleId,
                latitude,
                longitude,
                altitude,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                Map.of()
        );
    }

}