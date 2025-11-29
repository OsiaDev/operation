package co.cetad.umas.operation.infrastructure.web.mapper;

import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Mapper funcional de modelo de dominio a DTO de respuesta
 * Mantiene separación entre capas
 *
 * REFACTORIZACIÓN: Ahora incluye información completa de cada dron asignado
 * Recibe un Map de droneId -> Drone para evitar N+1 queries
 */
public final class MissionResponseMapper {

    private MissionResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte Mission + asignaciones + drones a MissionResponse
     *
     * OPTIMIZACIÓN: Recibe Map de drones precargados para evitar N+1 queries
     *
     * @param mission Misión del dominio
     * @param assignments Lista de asignaciones de drones a la misión
     * @param dronesMap Map de droneId -> Drone (precargado)
     * @return MissionResponse con toda la información
     */
    public static MissionResponse toResponse(
            Mission mission,
            List<DroneMissionAssignment> assignments,
            Map<String, Drone> dronesMap
    ) {
        // Mapear asignaciones de drones con información completa
        List<MissionResponse.DroneAssignmentResponse> droneResponses = assignments.stream()
                .map(assignment -> {
                    Drone drone = dronesMap.get(assignment.droneId());

                    if (drone == null) {
                        // Si el dron no está en el map, crear response con solo IDs
                        // (no debería pasar, pero es un fallback seguro)
                        return new MissionResponse.DroneAssignmentResponse(
                                UUID.fromString(assignment.id()),
                                UUID.fromString(assignment.droneId()),
                                "Unknown",
                                "Unknown",
                                "Unknown",
                                null,
                                "Unknown",
                                "UNKNOWN",
                                java.math.BigDecimal.ZERO,
                                null,
                                null,
                                assignment.routeId() != null ? UUID.fromString(assignment.routeId()) : null,
                                assignment.hasRoute()
                        );
                    }

                    // Mapear con información completa del dron
                    return new MissionResponse.DroneAssignmentResponse(
                            UUID.fromString(assignment.id()),
                            UUID.fromString(drone.id()),
                            drone.name(),
                            drone.vehicleId(),
                            drone.model(),
                            drone.description(),
                            drone.serialNumber(),
                            drone.status().name(),
                            drone.flightHours(),
                            drone.createdAt(),
                            drone.updatedAt(),
                            assignment.routeId() != null ? UUID.fromString(assignment.routeId()) : null,
                            assignment.hasRoute()
                    );
                })
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
    }

    /**
     * Convierte Mission + asignaciones a MissionResponse
     * SIN información de drones (solo IDs)
     *
     * Útil cuando no necesitas cargar los drones completos
     */
    public static MissionResponse toResponseWithoutDroneDetails(
            Mission mission,
            List<DroneMissionAssignment> assignments
    ) {
        return toResponse(mission, assignments, Map.of());
    }

    /**
     * Convierte Mission SIN asignaciones a MissionResponse
     * Útil cuando solo necesitas la misión sin información de drones
     */
    public static MissionResponse toResponseWithoutAssignments(Mission mission) {
        return toResponse(mission, List.of(), Map.of());
    }

}