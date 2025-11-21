package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de persistencia para operaciones de drones
 * Implementa Persistable para controlar INSERT vs UPDATE
 *
 * Nota: Usa UUID para id, droneId y routeId para eficiencia en PostgreSQL
 */
@Table("drone_operation")
public record DroneOperationEntity(
        @Id
        UUID id,

        @Column("drone_id")
        UUID droneId,

        @Column("route_id")
        UUID routeId,

        @Column("status")
        String status,

        @Column("start_date")
        LocalDateTime startDate,

        @Column("created_at")
        LocalDateTime createdAt,

        @Column("updated_at")
        LocalDateTime updatedAt,

        @Transient
        boolean isNew
) implements Persistable<UUID> {

    /**
     * Constructor para nuevas entidades (INSERT)
     */
    public static DroneOperationEntity create(
            UUID id,
            UUID droneId,
            UUID routeId,
            String status,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new DroneOperationEntity(
                id, droneId, routeId, status, startDate, createdAt, updatedAt, true
        );
    }

    /**
     * Constructor para entidades desde BD (UPDATE)
     */
    public static DroneOperationEntity fromDatabase(
            UUID id,
            UUID droneId,
            UUID routeId,
            String status,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new DroneOperationEntity(
                id, droneId, routeId, status, startDate, createdAt, updatedAt, false
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}