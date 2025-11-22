package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de persistencia para órdenes de misión
 * Implementa Persistable para controlar INSERT vs UPDATE
 */
@Table("mission_order")
public record MissionOrderEntity(
        @Id
        UUID id,

        @Column("mission_id")
        UUID missionId,

        @Column("commander_name")
        String commanderName,

        @Column("created_at")
        LocalDateTime createdAt,

        @Column("decision_at")
        LocalDateTime decisionAt,

        @Transient
        boolean isNew
) implements Persistable<UUID> {

    /**
     * Constructor para creación de nuevas entidades (INSERT)
     */
    public static MissionOrderEntity create(
            UUID id,
            UUID missionId,
            String commanderName,
            LocalDateTime createdAt,
            LocalDateTime decisionAt
    ) {
        return new MissionOrderEntity(
                id, missionId, commanderName, createdAt, decisionAt, true
        );
    }

    /**
     * Constructor para entidades cargadas desde BD (UPDATE)
     */
    public static MissionOrderEntity fromDatabase(
            UUID id,
            UUID missionId,
            String commanderName,
            LocalDateTime createdAt,
            LocalDateTime decisionAt
    ) {
        return new MissionOrderEntity(
                id, missionId, commanderName, createdAt, decisionAt, false
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