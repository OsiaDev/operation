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
     * Convierte de dominio (String IDs) a entidad de persistencia (UUID IDs)
     */
    public static final Function<MissionOrder, MissionOrderEntity> toEntity = order ->
            MissionOrderEntity.create(
                    UUID.fromString(order.id()),
                    UUID.fromString(order.missionId()),
                    order.commanderName(),
                    order.createdAt(),
                    order.decisionAt()
            );

    /**
     * Convierte de entidad de persistencia (UUID IDs) a dominio (String IDs)
     */
    public static final Function<MissionOrderEntity, MissionOrder> toDomain = entity ->
            new MissionOrder(
                    entity.id().toString(),
                    entity.missionId().toString(),
                    entity.commanderName(),
                    entity.createdAt(),
                    entity.decisionAt()
            );

}