package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia para operaciones de drones
 * Implementa Persistable para controlar INSERT vs UPDATE
 */
@Table("drone_operation")
public record DroneOperationEntity(
        @Id
        String id,

        @Column("drone_id")
        String droneId,

        @Column("route_id")
        String routeId,

        @Column("start_date")
        LocalDateTime startDate,

        @Column("created_at")
        LocalDateTime createdAt,

        @Column("updated_at")
        LocalDateTime updatedAt,

        @Transient
        boolean isNew
) implements Persistable<String> {

    public static DroneOperationEntity create(
            String id,
            String droneId,
            String routeId,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new DroneOperationEntity(
                id, droneId, routeId, startDate, createdAt, updatedAt, true
        );
    }

    public static DroneOperationEntity fromDatabase(
            String id,
            String droneId,
            String routeId,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new DroneOperationEntity(
                id, droneId, routeId, startDate, createdAt, updatedAt, false
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