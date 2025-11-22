package co.cetad.umas.operation.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "drone_telemetry")
public class DroneTelemetryEntity implements Serializable, Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "vehicle_id")
    private String vehicleId;

    @Column(name = "telemetry_type")
    private String telemetryType;

    // Ubicación
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "accuracy")
    private Double accuracy;

    // Métricas
    @Column(name = "speed")
    private Double speed;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "battery_level")
    private Double batteryLevel;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "signal_strength")
    private Double signalStrength;

    // Timestamps
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Control persistencia
    @Transient
    private boolean isNew;

    @Override
    public boolean isNew() {
        return isNew;
    }

}