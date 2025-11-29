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

/**
 * Entidad JPA para la tabla MISSION
 *
 * REFACTORIZACIÓN: Misión independiente de los drones
 * Los drones se asignan a través de DroneMissionAssignmentEntity
 *
 * FECHAS:
 * - estimatedDate: Fecha estimada de ejecución (NOT NULL)
 * - startDate: Fecha real de inicio (NULLABLE)
 * - endDate: Fecha real de finalización (NULLABLE)
 */
@Getter
@Setter
@Entity
@Table(name = "mission")
public class MissionEntity implements Serializable, Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "mission_type", columnDefinition = "mission_origin", nullable = false)
    private MissionOrigin missionType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "state", columnDefinition = "mission_state", nullable = false)
    private MissionState state;

    /**
     * Fecha estimada de ejecución de la misión (NOT NULL)
     * Siempre debe existir al crear la misión
     */
    @Column(name = "estimated_date", nullable = false)
    private LocalDateTime estimatedDate;

    /**
     * Fecha real de inicio de ejecución (NULLABLE)
     * Se llena cuando la misión realmente inicia
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /**
     * Fecha real de finalización (NULLABLE)
     * Se llena cuando la misión realmente termina
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

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