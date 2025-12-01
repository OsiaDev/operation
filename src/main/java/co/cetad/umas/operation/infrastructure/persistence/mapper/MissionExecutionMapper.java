package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.MissionExecutionEntity;
import co.cetad.umas.operation.domain.model.vo.MissionExecution;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones String (dominio) ↔ UUID (persistencia)
 */
public final class MissionExecutionMapper {

    private MissionExecutionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<MissionExecution, MissionExecutionEntity> toEntity = execution -> {
        MissionExecutionEntity entity = new MissionExecutionEntity();

        entity.setId(UUID.fromString(execution.id()));
        entity.setMissionId(UUID.fromString(execution.missionId()));
        entity.setCommanderName(execution.commanderName());
        entity.setCreatedAt(execution.createdAt());
        entity.setDecisionAt(execution.decisionAt());

        // Marcar como nuevo para INSERT
        entity.setNew(true);

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID IDs → String IDs
     */
    public static final Function<MissionExecutionEntity, MissionExecution> toDomain = entity ->
            new MissionExecution(
                    entity.getId().toString(),
                    entity.getMissionId().toString(),
                    entity.getCommanderName(),
                    entity.getCreatedAt(),
                    entity.getDecisionAt()
            );

}