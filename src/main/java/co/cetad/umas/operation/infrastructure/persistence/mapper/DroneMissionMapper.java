package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneMissionEntity;
import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.Optional;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 */
public final class DroneMissionMapper {

    private DroneMissionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     */
    public static final Function<DroneMission, DroneMissionEntity> toEntity = mission ->
            DroneMissionEntity.create(
                    mission.id(),
                    mission.name(),
                    mission.droneId(),
                    mission.routeId(),
                    mission.operatorId(),
                    mission.missionType(),
                    mission.state(),
                    mission.startDate(),
                    mission.createdAt(),
                    mission.updatedAt()
            );

    /**
     * Convierte de entidad de persistencia a dominio
     */
    public static final Function<DroneMissionEntity, DroneMission> toDomain = entity ->
            new DroneMission(
                    entity.id(),
                    entity.name(),
                    entity.droneId(),
                    entity.routeId(),
                    entity.operatorId(),
                    entity.missionType(),
                    entity.state(),
                    entity.startDate(),
                    entity.createdAt(),
                    entity.updatedAt()
            );

    /**
     * Parsea el tipo de misión de forma segura
     */
    private static MissionOrigin parseMissionOrigin(String type) {
        return Optional.ofNullable(type)
                .map(String::toUpperCase)
                .map(t -> {
                    try {
                        return MissionOrigin.valueOf(t);
                    } catch (IllegalArgumentException e) {
                        return MissionOrigin.AUTOMATICA;
                    }
                })
                .orElse(MissionOrigin.AUTOMATICA);
    }

    /**
     * Parsea el estado de la misión de forma segura
     */
    private static MissionState parseMissionState(String state) {
        return Optional.ofNullable(state)
                .map(String::toUpperCase)
                .map(s -> {
                    try {
                        return MissionState.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return MissionState.PENDIENTE_APROBACION;
                    }
                })
                .orElse(MissionState.PENDIENTE_APROBACION);
    }

}