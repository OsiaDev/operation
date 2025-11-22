package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de persistencia para misiones de drones
 * Implementa Persistable para controlar INSERT vs UPDATE
 *
 * IMPORTANTE: Los campos de tipo ENUM (mission_type y state) usan converters personalizados
 * definidos en R2dbcConvertersConfig para mapear correctamente a los tipos ENUM de PostgreSQL
 */
@Table("drone_mission")
public record DroneMissionEntity(
        @Id
        UUID id,

        @Column("name")
        String name,

        @Column("drone_id")
        UUID droneId,

        @Column("route_id")
        UUID routeId,

        @Column("operator_id")
        UUID operatorId,

        /**
         * Tipo ENUM mission_origin en PostgreSQL
         * Los converters en R2dbcConvertersConfig manejan la conversión String ↔ ENUM
         */
        @Column("mission_type")
        MissionOrigin missionType,

        /**
         * Tipo ENUM mission_state en PostgreSQL
         * Los converters en R2dbcConvertersConfig manejan la conversión String ↔ ENUM
         */
        @Column("state")
        MissionState state,

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
     * Constructor para creación de nuevas entidades (INSERT)
     */
    public static DroneMissionEntity create(
            UUID id,
            String name,
            UUID droneId,
            UUID routeId,
            UUID operatorId,
            MissionOrigin missionType,
            MissionState state,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new DroneMissionEntity(
                id, name, droneId, routeId, operatorId,
                missionType, state, startDate, createdAt, updatedAt,
                true
        );
    }

    /**
     * Constructor para entidades cargadas desde BD (UPDATE)
     */
    public static DroneMissionEntity fromDatabase(
            UUID id,
            String name,
            UUID droneId,
            UUID routeId,
            UUID operatorId,
            MissionOrigin missionType,
            MissionState state,
            LocalDateTime startDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new DroneMissionEntity(
                id, name, droneId, routeId, operatorId,
                missionType, state, startDate, createdAt, updatedAt,
                false
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