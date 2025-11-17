package co.cetad.umas.operation.infrastructure.web.mapper;

import co.cetad.umas.operation.domain.model.dto.TelemetryResponse;
import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;

import java.util.function.Function;

/**
 * Mapper funcional de modelo de dominio a DTO de respuesta
 * Mantiene separación entre capas
 */
public final class TelemetryResponseMapper {

    private TelemetryResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte entidad de dominio a DTO de respuesta
     */
    public static final Function<DroneTelemetry, TelemetryResponse> toResponse = telemetry ->
            new TelemetryResponse(
                    telemetry.id(),
                    telemetry.vehicleId(),
                    telemetry.type().name(),
                    new TelemetryResponse.LocationData(
                            telemetry.location().latitude(),
                            telemetry.location().longitude(),
                            telemetry.location().altitude(),
                            telemetry.location().accuracy()
                    ),
                    new TelemetryResponse.MetricsData(
                            telemetry.metrics().speed(),
                            telemetry.metrics().heading(),
                            telemetry.metrics().batteryLevel(),
                            telemetry.metrics().temperature(),
                            telemetry.metrics().signalStrength()
                    ),
                    telemetry.timestamp(),
                    telemetry.createdAt()
            );

}