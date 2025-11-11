package co.cetad.umas.operation.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Value Object que representa un evento de telemetría recibido desde Kafka
 * Adaptado al nuevo formato con vehicleId y campos planos
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

        validateLatitude(latitude);
        validateLongitude(longitude);

        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        // additionalFields puede ser null, lo manejamos
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
     * Útil para mantener compatibilidad con el dominio interno
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
     * Útil para mantener compatibilidad con el dominio interno
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
     * Verifica si el evento requiere una alerta
     */
    public boolean requiresAlert() {
        return isBatteryCritical() ||
                isSignalWeak() ||
                isOverheating() ||
                isSatelliteLoss();
    }

    /**
     * Obtiene la razón de la alerta
     */
    public String getAlertReason() {
        if (isBatteryCritical()) {
            return "Critical battery level: " + batteryLevel + "%";
        }
        if (isSignalWeak()) {
            Double signal = extractSignalStrength();
            return "Weak signal strength: " + (signal != null ? signal + " dBm" : "unknown");
        }
        if (isOverheating()) {
            Double temp = extractTemperature();
            return "Overheating: " + (temp != null ? temp + "°C" : "unknown");
        }
        if (isSatelliteLoss()) {
            return "Low satellite count: " + satelliteCount;
        }
        return null;
    }

    /**
     * Alias para vehicleId (compatibilidad con código existente)
     */
    public String vehicleId() {
        return vehicleId;
    }

    // ========== Métodos de validación y extracción ==========

    private boolean isBatteryCritical() {
        return batteryLevel != null && batteryLevel < 10;
    }

    private boolean isSignalWeak() {
        Double signal = extractSignalStrength();
        return signal != null && signal < -90;
    }

    private boolean isOverheating() {
        Double temp = extractTemperature();
        return temp != null && temp > 70;
    }

    private boolean isSatelliteLoss() {
        return satelliteCount != null && satelliteCount < 4;
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

    private static void validateLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90, got: " + latitude
            );
        }
    }

    private static void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180, got: " + longitude
            );
        }
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