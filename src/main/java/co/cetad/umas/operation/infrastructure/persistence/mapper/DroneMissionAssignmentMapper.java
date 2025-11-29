package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneMissionAssignmentEntity;
import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio DroneMissionAssignment
 * y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones:
 * - String (dominio) ↔ UUID (persistencia) para IDs
 */
public final class DroneMissionAssignmentMapper {

    private DroneMissionAssignmentMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String IDs → UUID IDs
     */
    public static final Function<DroneMissionAssignment, DroneMissionAssignmentEntity> toEntity =
            assignment -> {
                DroneMissionAssignmentEntity entity = new DroneMissionAssignmentEntity();

                entity.setId(UUID.fromString(assignment.id()));
                entity.setMissionId(UUID.fromString(assignment.missionId()));
                entity.setDroneId(UUID.fromString(assignment.droneId()));

                // routeId es opcional
                if (assignment.routeId() != null && !assignment.routeId().isBlank()) {
                    entity.setRouteId(UUID.fromString(assignment.routeId()));
                }

                entity.setCreatedAt(assignment.createdAt());
                entity.setUpdatedAt(assignment.updatedAt());

                // Marcar como nuevo para INSERT
                entity.setNew(assignment.isNew());

                return entity;
            };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID IDs → String IDs
     */
    public static final Function<DroneMissionAssignmentEntity, DroneMissionAssignment> toDomain =
            entity ->
                    new DroneMissionAssignment(
                            entity.getId().toString(),
                            entity.getMissionId().toString(),
                            entity.getDroneId().toString(),
                            entity.getRouteId() != null ? entity.getRouteId().toString() : null,
                            entity.getCreatedAt(),
                            entity.getUpdatedAt(),
                            entity.isNew()
                    );

}