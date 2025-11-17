package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneOperationEntity;
import co.cetad.umas.operation.domain.model.vo.DroneOperation;

import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 */
public final class DroneOperationMapper {

    private DroneOperationMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     */
    public static final Function<DroneOperation, DroneOperationEntity> toEntity = operation ->
            DroneOperationEntity.create(
                    operation.id(),
                    operation.droneId(),
                    operation.routeId(),
                    operation.startDate(),
                    operation.createdAt(),
                    operation.updatedAt()
            );

    /**
     * Convierte de entidad de persistencia a dominio
     */
    public static final Function<DroneOperationEntity, DroneOperation> toDomain = entity ->
            new DroneOperation(
                    entity.id(),
                    entity.droneId(),
                    entity.routeId(),
                    entity.startDate(),
                    entity.createdAt(),
                    entity.updatedAt()
            );

}