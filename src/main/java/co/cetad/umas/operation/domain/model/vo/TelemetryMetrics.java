package co.cetad.umas.operation.domain.model.vo;

/**
 * Value Object simplificado que encapsula métricas de telemetría
 * Solo almacena datos sin validaciones de negocio
 */
public record TelemetryMetrics(
        Double speed,           // m/s
        Double heading,         // grados (0-360)
        Double batteryLevel,    // porcentaje (0-100)
        Double temperature,     // celsius
        Double signalStrength   // dBm
) {

    public static TelemetryMetrics empty() {
        return new TelemetryMetrics(null, null, null, null, null);
    }

}