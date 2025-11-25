package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneMissionEntity;
import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones:
 * - String (dominio) ↔ UUID (persistencia) para IDs
 */
public final class DroneMissionMapper {

    private DroneMissionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<DroneMission, DroneMissionEntity> toEntity = mission -> {
        DroneMissionEntity entity = new DroneMissionEntity();

        entity.setId(UUID.fromString(mission.id()));
        entity.setName(mission.name());
        entity.setDroneId(UUID.fromString(mission.droneId()));

        // routeId es opcional
        if (mission.routeId() != null && !mission.routeId().isBlank()) {
            entity.setRouteId(UUID.fromString(mission.routeId()));
        }

        entity.setOperatorId(UUID.fromString(mission.operatorId()));
        entity.setMissionType(mission.missionType());
        entity.setState(mission.state());
        entity.setStartDate(mission.startDate());
        entity.setCreatedAt(mission.createdAt());
        entity.setUpdatedAt(mission.updatedAt());

        // Marcar como nuevo para INSERT
        entity.setNew(mission.isNew());

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID IDs → String IDs
     */
    public static final Function<DroneMissionEntity, DroneMission> toDomain = entity ->
            new DroneMission(
                    entity.getId().toString(),
                    entity.getName(),
                    entity.getDroneId().toString(),
                    entity.getRouteId() != null ? entity.getRouteId().toString() : null,
                    entity.getOperatorId().toString(),
                    entity.getMissionType(),
                    entity.getState(),
                    entity.getStartDate(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt(),
                    entity.isNew()
            );

}