package co.cetad.umas.operation.infrastructure.web.mapper;

import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper funcional de modelo de dominio a DTO de respuesta
 * Mantiene separación entre capas
 *
 * ACTUALIZACIÓN: Ahora incluye información de auditoría completa
 * (quién creó, aprobó, ejecutó y finalizó la misión)
 */
public final class MissionResponseMapper {

    private MissionResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte Mission + asignaciones + drones + auditoría a MissionResponse
     *
     * OPTIMIZACIÓN: Recibe Map de drones precargados para evitar N+1 queries
     *
     * @param mission Misión del dominio
     * @param assignments Lista de asignaciones de drones a la misión
     * @param dronesMap Map de droneId -> Drone (precargado)
     * @param order Orden de misión (quién creó)
     * @param approval Aprobación de misión (quién aprobó)
     * @param execution Ejecución de misión (quién ejecutó)
     * @param finalization Finalización de misión (quién finalizó)
     * @return MissionResponse con toda la información
     */
    public static MissionResponse toResponse(
            Mission mission,
            List<DroneMissionAssignment> assignments,
            Map<String, Drone> dronesMap,
            MissionOrder order,
            MissionApproval approval,
            MissionExecution execution,
            MissionFinalization finalization
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
                // Auditoría
                order != null ? order.commanderName() : null,
                approval != null ? approval.commanderName() : null,
                execution != null ? execution.commanderName() : null,
                finalization != null ? finalization.commanderName() : null,
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
     * SIN información de drones (solo IDs) ni auditoría
     *
     * Útil cuando no necesitas cargar los drones completos
     */
    public static MissionResponse toResponseWithoutDroneDetails(
            Mission mission,
            List<DroneMissionAssignment> assignments
    ) {
        return toResponse(mission, assignments, Map.of(), null, null, null, null);
    }

    /**
     * Convierte Mission SIN asignaciones a MissionResponse
     * Útil cuando solo necesitas la misión sin información de drones ni auditoría
     */
    public static MissionResponse toResponseWithoutAssignments(Mission mission) {
        return toResponse(mission, List.of(), Map.of(), null, null, null, null);
    }

}