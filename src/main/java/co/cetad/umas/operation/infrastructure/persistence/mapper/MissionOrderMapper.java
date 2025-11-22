package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.MissionOrderEntity;
import co.cetad.umas.operation.domain.model.vo.MissionOrder;

import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 */
public final class MissionOrderMapper {

    private MissionOrderMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     */
    public static final Function<MissionOrder, MissionOrderEntity> toEntity = order ->
            MissionOrderEntity.create(
                    order.id(),
                    order.missionId(),
                    order.commanderName(),
                    order.createdAt(),
                    order.decisionAt()
            );

    /**
     * Convierte de entidad de persistencia a dominio
     */
    public static final Function<MissionOrderEntity, MissionOrder> toDomain = entity ->
            new MissionOrder(
                    entity.id(),
                    entity.missionId(),
                    entity.commanderName(),
                    entity.createdAt(),
                    entity.decisionAt()
            );

}