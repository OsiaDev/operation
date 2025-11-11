package co.cetad.umas.operation.domain.model.vo;

/**
 * Value Object que encapsula las métricas de telemetría del dron
 */
public record TelemetryMetrics(
        Double speed,           // m/s
        Double heading,         // grados (0-360)
        Double batteryLevel,    // porcentaje (0-100)
        Double temperature,     // celsius
        Double signalStrength   // dBm
) {
    public TelemetryMetrics {
        validateMetric(speed, 0, 100, "Speed");
        validateMetric(heading, 0, 360, "Heading");
        validateMetric(batteryLevel, 0, 100, "Battery level");
        validateMetric(temperature, -50, 100, "Temperature");
        validateMetric(signalStrength, -120, 0, "Signal strength");
    }

    private static void validateMetric(Double value, double min, double max, String fieldName) {
        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %.2f and %.2f", fieldName, min, max)
            );
        }
    }

    public boolean isBatteryLow() {
        return batteryLevel != null && batteryLevel < 20;
    }

    public boolean isBatteryCritical() {
        return batteryLevel != null && batteryLevel < 10;
    }

    public boolean isSignalWeak() {
        return signalStrength != null && signalStrength < -90;
    }

    public boolean isOverheating() {
        return temperature != null && temperature > 70;
    }

    public static TelemetryMetrics empty() {
        return new TelemetryMetrics(null, null, null, null, null);
    }

}