package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.model.entity.DroneTelemetryEntity;
import co.cetad.umas.operation.domain.model.entity.TelemetryType;
import co.cetad.umas.operation.domain.model.vo.GeoLocation;
import co.cetad.umas.operation.domain.model.vo.TelemetryMetrics;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Conversiones:
 * - String ID (dominio) ↔ UUID (persistencia)
 * - Value Objects (dominio) ↔ Campos planos (persistencia)
 */
public final class DroneTelemetryMapper {

    private DroneTelemetryMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String ID → UUID
     */
    public static final Function<DroneTelemetry, DroneTelemetryEntity> toEntity = telemetry -> {
        DroneTelemetryEntity entity = new DroneTelemetryEntity();

        entity.setId(UUID.fromString(telemetry.id()));
        entity.setVehicleId(telemetry.vehicleId());
        entity.setTelemetryType(telemetry.type().name());

        // GeoLocation → campos planos
        entity.setLatitude(telemetry.location().latitude());
        entity.setLongitude(telemetry.location().longitude());
        entity.setAltitude(telemetry.location().altitude());
        entity.setAccuracy(telemetry.location().accuracy());

        // TelemetryMetrics → campos planos
        entity.setSpeed(telemetry.metrics().speed());
        entity.setHeading(telemetry.metrics().heading());
        entity.setBatteryLevel(telemetry.metrics().batteryLevel());
        entity.setTemperature(telemetry.metrics().temperature());
        entity.setSignalStrength(telemetry.metrics().signalStrength());

        entity.setTimestamp(telemetry.timestamp());
        entity.setCreatedAt(telemetry.createdAt());

        // Marcar como nuevo para INSERT
        entity.setNew(true);

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID → String ID
     */
    public static final Function<DroneTelemetryEntity, DroneTelemetry> toDomain = entity -> {
        GeoLocation location = new GeoLocation(
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getAltitude(),
                entity.getAccuracy()
        );

        TelemetryMetrics metrics = new TelemetryMetrics(
                entity.getSpeed(),
                entity.getHeading(),
                entity.getBatteryLevel(),
                entity.getTemperature(),
                entity.getSignalStrength()
        );

        return new DroneTelemetry(
                entity.getId().toString(),
                entity.getVehicleId(),
                TelemetryType.valueOf(entity.getTelemetryType()),
                location,
                metrics,
                entity.getTimestamp(),
                entity.getCreatedAt()
        );
    };

}