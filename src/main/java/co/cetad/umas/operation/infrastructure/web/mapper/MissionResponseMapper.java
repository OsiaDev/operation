package co.cetad.umas.operation.infrastructure.web.mapper;

import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional de modelo de dominio a DTO de respuesta
 * Mantiene separación entre capas
 *
 * Convierte String IDs (dominio) → UUID IDs (response)
 */
public final class MissionResponseMapper {

    private MissionResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte entidad de dominio (String IDs) a DTO de respuesta (UUID IDs)
     */
    public static final Function<DroneMission, MissionResponse> toResponse = mission ->
            MissionResponse.from(
                    parseUUID(mission.id()),
                    mission.name(),
                    parseUUID(mission.droneId()),
                    parseNullableUUID(mission.routeId()),
                    parseUUID(mission.operatorId()),
                    mission.missionType(),
                    mission.state(),
                    mission.startDate(),
                    mission.createdAt(),
                    mission.updatedAt(),
                    mission.hasRoute(),
                    mission.hasName(),
                    mission.isScheduledForFuture(),
                    mission.isManual(),
                    mission.isPendingApproval()
            );

    /**
     * Convierte String a UUID
     * Lanza excepción si el String no es un UUID válido
     */
    private static UUID parseUUID(String uuidString) {
        return UUID.fromString(uuidString);
    }

    /**
     * Convierte String a UUID, permitiendo null
     * Retorna null si el String es null o vacío
     */
    private static UUID parseNullableUUID(String uuidString) {
        return Optional.ofNullable(uuidString)
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .orElse(null);
    }

}