package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.MissionApprovalEntity;
import co.cetad.umas.operation.domain.model.vo.MissionApproval;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones String (dominio) ↔ UUID (persistencia)
 */
public final class MissionApprovalMapper {

    private MissionApprovalMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<MissionApproval, MissionApprovalEntity> toEntity = approval -> {
        MissionApprovalEntity entity = new MissionApprovalEntity();

        entity.setId(UUID.fromString(approval.id()));
        entity.setMissionId(UUID.fromString(approval.missionId()));
        entity.setCommanderName(approval.commanderName());
        entity.setCreatedAt(approval.createdAt());
        entity.setDecisionAt(approval.decisionAt());

        // Marcar como nuevo para INSERT
        entity.setNew(true);

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID IDs → String IDs
     */
    public static final Function<MissionApprovalEntity, MissionApproval> toDomain = entity ->
            new MissionApproval(
                    entity.getId().toString(),
                    entity.getMissionId().toString(),
                    entity.getCommanderName(),
                    entity.getCreatedAt(),
                    entity.getDecisionAt()
            );

}