package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia para telemetría de drones
 * Implementa Persistable para controlar el comportamiento de INSERT vs UPDATE
 * ya que generamos UUIDs manualmente en el dominio
 */
@Table("drone_telemetry")
public record DroneTelemetryEntity(
        @Id
        String id,

        @Column("vehicle_id")
        String vehicleId,

        @Column("telemetry_type")
        String telemetryType,

        // Ubicación
        @Column("latitude")
        Double latitude,

        @Column("longitude")
        Double longitude,

        @Column("altitude")
        Double altitude,

        @Column("accuracy")
        Double accuracy,

        // Métricas
        @Column("speed")
        Double speed,

        @Column("heading")
        Double heading,

        @Column("battery_level")
        Double batteryLevel,

        @Column("temperature")
        Double temperature,

        @Column("signal_strength")
        Double signalStrength,

        // Timestamps
        @Column("timestamp")
        LocalDateTime timestamp,

        @Column("created_at")
        LocalDateTime createdAt,

        // Flag transient para control de persistencia
        @Transient
        boolean isNew
) implements Persistable<String> {

    /**
     * Constructor para creación de nuevas entidades (INSERT)
     * Marca la entidad como nueva para forzar INSERT
     */
    public static DroneTelemetryEntity create(
            String id,
            String vehicleId,
            String telemetryType,
            Double latitude,
            Double longitude,
            Double altitude,
            Double accuracy,
            Double speed,
            Double heading,
            Double batteryLevel,
            Double temperature,
            Double signalStrength,
            LocalDateTime timestamp
    ) {
        return new DroneTelemetryEntity(
                id,
                vehicleId,
                telemetryType,
                latitude,
                longitude,
                altitude,
                accuracy,
                speed,
                heading,
                batteryLevel,
                temperature,
                signalStrength,
                timestamp,
                LocalDateTime.now(),
                true  // ✅ Marca como nueva para INSERT
        );
    }

    /**
     * Constructor para entidades cargadas desde BD (UPDATE)
     * Marca la entidad como existente
     */
    public static DroneTelemetryEntity fromDatabase(
            String id,
            String vehicleId,
            String telemetryType,
            Double latitude,
            Double longitude,
            Double altitude,
            Double accuracy,
            Double speed,
            Double heading,
            Double batteryLevel,
            Double temperature,
            Double signalStrength,
            LocalDateTime timestamp,
            LocalDateTime createdAt
    ) {
        return new DroneTelemetryEntity(
                id,
                vehicleId,
                telemetryType,
                latitude,
                longitude,
                altitude,
                accuracy,
                speed,
                heading,
                batteryLevel,
                temperature,
                signalStrength,
                timestamp,
                createdAt,
                false  // ✅ Marca como existente para UPDATE
        );
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}