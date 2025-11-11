package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.model.entity.DroneTelemetryEntity;
import co.cetad.umas.operation.domain.model.entity.TelemetryType;
import co.cetad.umas.operation.domain.model.vo.GeoLocation;
import co.cetad.umas.operation.domain.model.vo.TelemetryMetrics;

import java.util.Optional;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 */
public final class DroneTelemetryMapper {

    private DroneTelemetryMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     */
    public static final Function<DroneTelemetry, DroneTelemetryEntity> toEntity = telemetry ->
            DroneTelemetryEntity.create(
                    telemetry.id(),
                    telemetry.vehicleId(),
                    telemetry.type().name(),
                    telemetry.location().latitude(),
                    telemetry.location().longitude(),
                    telemetry.location().altitude(),
                    telemetry.location().accuracy(),
                    telemetry.metrics().speed(),
                    telemetry.metrics().heading(),
                    telemetry.metrics().batteryLevel(),
                    telemetry.metrics().temperature(),
                    telemetry.metrics().signalStrength(),
                    telemetry.timestamp()
            );

    /**
     * Convierte de entidad de persistencia a dominio
     */
    public static final Function<DroneTelemetryEntity, DroneTelemetry> toDomain = entity ->
            new DroneTelemetry(
                    entity.id(),
                    entity.vehicleId(),
                    parseTelemetryType(entity.telemetryType()),
                    new GeoLocation(
                            entity.latitude(),
                            entity.longitude(),
                            entity.altitude(),
                            entity.accuracy()
                    ),
                    new TelemetryMetrics(
                            entity.speed(),
                            entity.heading(),
                            entity.batteryLevel(),
                            entity.temperature(),
                            entity.signalStrength()
                    ),
                    entity.timestamp(),
                    entity.createdAt()
            );

    /**
     * Parsea el tipo de telemetría de forma segura
     */
    private static TelemetryType parseTelemetryType(String type) {
        return Optional.ofNullable(type)
                .map(String::toUpperCase)
                .map(t -> {
                    try {
                        return TelemetryType.valueOf(t);
                    } catch (IllegalArgumentException e) {
                        return TelemetryType.FULL_TELEMETRY;
                    }
                })
                .orElse(TelemetryType.FULL_TELEMETRY);
    }

}