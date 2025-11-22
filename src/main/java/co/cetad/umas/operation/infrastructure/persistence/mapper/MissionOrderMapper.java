package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.MissionOrderEntity;
import co.cetad.umas.operation.domain.model.vo.MissionOrder;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones String (dominio) ↔ UUID (persistencia)
 */
public final class MissionOrderMapper {

    private MissionOrderMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<MissionOrder, MissionOrderEntity> toEntity = order -> {
        MissionOrderEntity entity = new MissionOrderEntity();

        entity.setId(UUID.fromString(order.id()));
        entity.setMissionId(UUID.fromString(order.missionId()));
        entity.setCommanderName(order.commanderName());
        entity.setCreatedAt(order.createdAt());
        entity.setDecisionAt(order.decisionAt());

        // Marcar como nuevo para INSERT
        entity.setNew(true);

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID IDs → String IDs
     */
    public static final Function<MissionOrderEntity, MissionOrder> toDomain = entity ->
            new MissionOrder(
                    entity.getId().toString(),
                    entity.getMissionId().toString(),
                    entity.getCommanderName(),
                    entity.getCreatedAt(),
                    entity.getDecisionAt()
            );

}