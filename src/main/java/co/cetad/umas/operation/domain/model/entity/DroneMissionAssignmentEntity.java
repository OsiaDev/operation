package co.cetad.umas.operation.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para la tabla DRONE_MISSION
 *
 * REFACTORIZACIÓN: Asignación de drones a misiones
 * Relación muchos-a-muchos entre misiones y drones
 *
 * UNIQUE CONSTRAINT: (mission_id, drone_id)
 * Un dron NO puede estar asignado dos veces a la misma misión
 */
@Getter
@Setter
@Entity
@Table(
        name = "drone_mission",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_drone_per_mission",
                columnNames = {"mission_id", "drone_id"}
        )
)
public class DroneMissionAssignmentEntity implements Serializable, Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Column(name = "drone_id", nullable = false)
    private UUID droneId;

    @Column(name = "route_id")
    private UUID routeId;

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