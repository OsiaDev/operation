package co.cetad.umas.operation.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "drone_mission")
public class DroneMissionEntity implements Serializable, Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "drone_id")
    private UUID droneId;

    @Column(name = "route_id")
    private UUID routeId;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mission_type", columnDefinition = "mission_origin")
    private MissionOrigin missionType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "state", columnDefinition = "mission_state")
    private MissionState state;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

}