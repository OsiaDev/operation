package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
    /**
     * Constructor para creación con valores generados
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
                LocalDateTime.now()
        );
    }

}