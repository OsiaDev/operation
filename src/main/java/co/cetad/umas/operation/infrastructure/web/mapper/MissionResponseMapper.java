package co.cetad.umas.operation.infrastructure.web.mapper;

import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Mapper funcional de modelo de dominio a DTO de respuesta
 * Mantiene separación entre capas
 *
 * REFACTORIZACIÓN: Ahora mapea Mission + List<DroneMissionAssignment>
 */
public final class MissionResponseMapper {

    private MissionResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte Mission + sus asignaciones de drones a MissionResponse
     *
     * @param mission Misión del dominio
     * @param assignments Lista de asignaciones de drones a la misión
     * @return MissionResponse con toda la información
     */
    public static final BiFunction<Mission, List<DroneMissionAssignment>, MissionResponse> toResponse =
            (mission, assignments) -> {
                // Mapear asignaciones de drones
                List<MissionResponse.DroneAssignmentResponse> droneResponses = assignments.stream()
                        .map(assignment -> new MissionResponse.DroneAssignmentResponse(
                                UUID.fromString(assignment.id()),
                                UUID.fromString(assignment.droneId()),
                                assignment.routeId() != null ? UUID.fromString(assignment.routeId()) : null,
                                assignment.hasRoute()
                        ))
                        .toList();

                return new MissionResponse(
                        UUID.fromString(mission.id()),
                        mission.name(),
                        UUID.fromString(mission.operatorId()),
                        mission.missionType(),
                        mission.state(),
                        mission.estimatedDate(),
                        mission.startDate(),
                        mission.endDate(),
                        droneResponses,
                        mission.createdAt(),
                        mission.updatedAt(),
                        mission.hasName(),
                        mission.isScheduledForFuture(),
                        mission.isManual(),
                        mission.isPendingApproval(),
                        mission.isInProgress(),
                        mission.hasStarted(),
                        mission.hasEnded(),
                        droneResponses.size()
                );
            };

    /**
     * Convierte Mission SIN asignaciones a MissionResponse
     * Útil cuando solo necesitas la misión sin información de drones
     */
    public static MissionResponse toResponseWithoutAssignments(Mission mission) {
        return toResponse.apply(mission, List.of());
    }

}