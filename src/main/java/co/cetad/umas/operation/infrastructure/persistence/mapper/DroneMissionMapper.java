package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneMissionEntity;
import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones:
 * - String (dominio) ↔ UUID (persistencia) para IDs
 * - MissionOrigin/MissionState (dominio) ↔ String (persistencia) para ENUMs
 */
public final class DroneMissionMapper {

    private DroneMissionMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     *
     * Conversiones:
     * - String ID → UUID ID
     * - MissionOrigin enum → String
     * - MissionState enum → String
     */
    public static final Function<DroneMission, DroneMissionEntity> toEntity = mission ->
            DroneMissionEntity.create(
                    UUID.fromString(mission.id()),
                    mission.name(),
                    UUID.fromString(mission.droneId()),
                    parseUUID(mission.routeId()),
                    UUID.fromString(mission.operatorId()),
                    mission.missionType(),  // ✅ Enum → String
                    mission.state(),         // ✅ Enum → String
                    mission.startDate(),
                    mission.createdAt(),
                    mission.updatedAt()
            );

    /**
     * Convierte de entidad de persistencia a dominio
     *
     * Conversiones:
     * - UUID ID → String ID
     * - String → MissionOrigin enum
     * - String → MissionState enum
     */
    public static final Function<DroneMissionEntity, DroneMission> toDomain = entity ->
            new DroneMission(
                    entity.id().toString(),
                    entity.name(),
                    entity.droneId().toString(),
                    formatUUID(entity.routeId()),
                    entity.operatorId().toString(),
                    entity.missionType(),  // ✅ String → Enum
                    entity.state(),          // ✅ String → Enum
                    entity.startDate(),
                    entity.createdAt(),
                    entity.updatedAt()
            );

    /**
     * Convierte String a UUID, retorna null si el String es null o vacío
     */
    private static UUID parseUUID(String uuidString) {
        return Optional.ofNullable(uuidString)
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .orElse(null);
    }

    /**
     * Convierte UUID a String, retorna null si el UUID es null
     */
    private static String formatUUID(UUID uuid) {
        return Optional.ofNullable(uuid)
                .map(UUID::toString)
                .orElse(null);
    }

    /**
     * Convierte String a MissionOrigin enum de forma segura
     * Retorna AUTOMATICA por defecto si hay error
     */
    private static MissionOrigin parseMissionOrigin(String originString) {
        return Optional.ofNullable(originString)
                .map(String::toUpperCase)
                .map(s -> {
                    try {
                        return MissionOrigin.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return MissionOrigin.AUTOMATICA;
                    }
                })
                .orElse(MissionOrigin.AUTOMATICA);
    }

    /**
     * Convierte String a MissionState enum de forma segura
     * Retorna PENDIENTE_APROBACION por defecto si hay error
     */
    private static MissionState parseMissionState(String stateString) {
        return Optional.ofNullable(stateString)
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