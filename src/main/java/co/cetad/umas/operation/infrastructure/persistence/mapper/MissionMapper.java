package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.MissionEntity;
import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio Mission y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones:
 * - String (dominio) ↔ UUID (persistencia) para IDs
 */
public final class MissionMapper {

    private MissionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<Mission, MissionEntity> toEntity = mission -> {
        MissionEntity entity = new MissionEntity();

        entity.setId(UUID.fromString(mission.id()));
        entity.setName(mission.name());
        entity.setOperatorId(UUID.fromString(mission.operatorId()));
        entity.setMissionType(mission.missionType());
        entity.setState(mission.state());
        entity.setEstimatedDate(mission.estimatedDate());
        entity.setStartDate(mission.startDate());
        entity.setEndDate(mission.endDate());
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
    public static final Function<MissionEntity, Mission> toDomain = entity ->
            new Mission(
                    entity.getId().toString(),
                    entity.getName(),
                    entity.getOperatorId().toString(),
                    entity.getMissionType(),
                    entity.getState(),
                    entity.getEstimatedDate(),
                    entity.getStartDate(),
                    entity.getEndDate(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt(),
                    entity.isNew()
            );

}