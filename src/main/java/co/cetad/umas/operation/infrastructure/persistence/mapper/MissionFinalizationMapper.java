package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.MissionFinalizationEntity;
import co.cetad.umas.operation.domain.model.vo.MissionFinalization;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones String (dominio) ↔ UUID (persistencia)
 */
public final class MissionFinalizationMapper {

    private MissionFinalizationMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<MissionFinalization, MissionFinalizationEntity> toEntity = finalization -> {
        MissionFinalizationEntity entity = new MissionFinalizationEntity();

        entity.setId(UUID.fromString(finalization.id()));
        entity.setMissionId(UUID.fromString(finalization.missionId()));
        entity.setCommanderName(finalization.commanderName());
        entity.setCreatedAt(finalization.createdAt());
        entity.setDecisionAt(finalization.decisionAt());

        // Marcar como nuevo para INSERT
        entity.setNew(true);

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID IDs → String IDs
     */
    public static final Function<MissionFinalizationEntity, MissionFinalization> toDomain = entity ->
            new MissionFinalization(
                    entity.getId().toString(),
                    entity.getMissionId().toString(),
                    entity.getCommanderName(),
                    entity.getCreatedAt(),
                    entity.getDecisionAt()
            );

}