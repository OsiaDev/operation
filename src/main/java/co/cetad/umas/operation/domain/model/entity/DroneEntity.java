package co.cetad.umas.operation.domain.model.entity;

import co.cetad.umas.operation.infrastructure.persistence.config.DroneStatusConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "drone")
public class DroneEntity implements Serializable, Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "vehicle_id", nullable = false, unique = true)
    private String vehicleId;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "description")
    private String description;

    @Column(name = "serial_number", nullable = false)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "drone_status", nullable = false)
    private DroneStatus status;

    @Column(name = "flight_hours", nullable = false)
    private BigDecimal flightHours;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

}