package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.dto.MissionExecutionCommand;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.*;
import co.cetad.umas.operation.domain.ports.in.ExecuteMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para ejecutar misiones con múltiples drones
 *
 * REFACTORIZACIÓN: Ahora coordina:
 * 1. Validación de la misión
 * 2. Búsqueda de TODOS los drones asignados a la misión
 * 3. Para cada dron: buscar ruta, extraer waypoints, publicar comando
 * 4. Actualización del estado de la misión a EN_EJECUCION
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteMissionService implements ExecuteMissionUseCase {

    private final MissionRepository missionRepository;
    private final DroneMissionAssignmentRepository assignmentRepository;
    private final KmlRouteRepository routeRepository;
    private final DroneRepository droneRepository;
    private final GeoJsonParserService geoJsonParserService;
    private final MissionExecutionPublisher executionPublisher;

    @Override
    public CompletableFuture<Mission> executeMission(
            String missionId,
            String commanderName
    ) {
        log.info("Executing mission: {} by commander: {}", missionId, commanderName);

        return validateInputs(missionId, commanderName)
                .thenCompose(validated -> missionRepository.findById(missionId))
                .thenCompose(missionOpt -> {
                    if (missionOpt.isEmpty()) {
                        log.error("❌ Mission not found: {}", missionId);
                        throw new MissionNotFoundException(
                                "Mission not found with id: " + missionId);
                    }

                    Mission mission = missionOpt.get();
                    return validateMissionState(mission)
                            .thenCompose(this::executeMissionFlow);
                })
                .thenApply(executedMission -> {
                    log.info("✅ Mission executed successfully: {} by commander: {}",
                            executedMission.id(), commanderName);
                    return executedMission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to execute mission: {}", missionId, throwable);
                    if (throwable.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) throwable.getCause();
                    }
                    throw new MissionExecutionException(
                            "Failed to execute mission: " + missionId, throwable);
                });
    }

    /**
     * Valida las entradas del caso de uso
     */
    private CompletableFuture<Void> validateInputs(String missionId, String commanderName) {
        return CompletableFuture.runAsync(() -> {
            if (missionId == null || missionId.isBlank()) {
                throw new IllegalArgumentException("Mission ID cannot be null or empty");
            }
            if (commanderName == null || commanderName.isBlank()) {
                throw new IllegalArgumentException("Commander name cannot be null or empty");
            }
        });
    }

    /**
     * Valida que la misión esté en estado APROBADA
     */
    private CompletableFuture<Mission> validateMissionState(Mission mission) {
        return CompletableFuture.supplyAsync(() -> {
            if (mission.state() != MissionState.APROBADA) {
                log.warn("⚠️ Mission {} is not in APROBADA state. Current state: {}",
                        mission.id(), mission.state());
                throw new InvalidMissionStateException(
                        String.format("Mission %s cannot be executed. Current state: %s. " +
                                        "Only missions in APROBADA state can be executed.",
                                mission.id(), mission.state()));
            }

            // Validar que la fecha de inicio ya debería haber pasado o ser ahora
            if (mission.isScheduledForFuture()) {
                log.warn("⚠️ Mission {} is scheduled for future: {}",
                        mission.id(), mission.estimatedDate());
                throw new MissionScheduledForFutureException(
                        String.format("Mission %s is scheduled for future date: %s. " +
                                        "Cannot execute missions scheduled for the future.",
                                mission.id(), mission.estimatedDate()));
            }

            return mission;
        });
    }

    /**
     * Flujo de ejecución completo:
     * 1. Buscar TODOS los drones asignados a la misión
     * 2. Para cada dron: obtener vehicleId, buscar ruta, extraer waypoints, publicar
     * 3. Actualizar estado de la misión a EN_EJECUCION con start_date
     */
    private CompletableFuture<Mission> executeMissionFlow(Mission mission) {
        log.info("🚀 Starting execution flow for mission: {}", mission.id());

        // Paso 1: Buscar todas las asignaciones de drones para esta misión
        return assignmentRepository.findByMissionId(mission.id())
                .thenCompose(assignments -> {
                    if (assignments.isEmpty()) {
                        log.error("❌ No drones assigned to mission: {}", mission.id());
                        throw new NoDronesAssignedException(
                                "No drones assigned to mission: " + mission.id());
                    }

                    log.info("Found {} drone(s) assigned to mission: {}",
                            assignments.size(), mission.id());

                    // Paso 2: Procesar cada asignación de dron
                    return processDroneAssignments(mission, assignments);
                })
                .thenCompose(processedMission -> {
                    // Paso 3: Actualizar estado de la misión a EN_EJECUCION
                    return updateMissionToInProgress(processedMission);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Error in execution flow for mission: {}",
                            mission.id(), throwable);
                    throw new MissionExecutionFlowException(
                            "Execution flow failed for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    /**
     * Procesa todas las asignaciones de drones en paralelo
     * Para cada dron: busca su vehicleId, ruta, waypoints y publica comando
     */
    private CompletableFuture<Mission> processDroneAssignments(
            Mission mission,
            List<DroneMissionAssignment> assignments
    ) {
        log.info("Processing {} drone assignment(s) for mission: {}",
                assignments.size(), mission.id());

        // Crear futures para procesar cada asignación
        List<CompletableFuture<Void>> assignmentFutures = assignments.stream()
                .map(assignment -> processSingleDroneAssignment(mission, assignment))
                .toList();

        // Esperar a que todas las asignaciones se procesen
        return CompletableFuture.allOf(
                        assignmentFutures.toArray(new CompletableFuture[0])
                )
                .thenApply(v -> {
                    log.info("✅ All {} drone(s) processed successfully for mission: {}",
                            assignments.size(), mission.id());
                    return mission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to process drone assignments for mission: {}",
                            mission.id(), throwable);
                    throw new DroneAssignmentProcessingException(
                            "Failed to process drone assignments for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    /**
     * Procesa una sola asignación de dron:
     * 1. Buscar dron para obtener vehicleId
     * 2. Si tiene ruta: buscar ruta, extraer waypoints
     * 3. Publicar comando de ejecución
     */
    private CompletableFuture<Void> processSingleDroneAssignment(
            Mission mission,
            DroneMissionAssignment assignment
    ) {
        log.debug("Processing assignment for drone: {} in mission: {}",
                assignment.droneId(), mission.id());

        // Paso 1: Buscar el dron para obtener vehicleId
        return droneRepository.findById(assignment.droneId())
                .thenCompose(droneOpt -> {
                    if (droneOpt.isEmpty()) {
                        log.error("❌ Drone not found: {}", assignment.droneId());
                        throw new DroneNotFoundException(
                                "Drone not found with id: " + assignment.droneId());
                    }

                    Drone drone = droneOpt.get();
                    log.debug("Found drone vehicleId: {} for assignment", drone.vehicleId());

                    // Paso 2: Si tiene ruta, procesarla; si no, enviar comando sin waypoints
                    if (assignment.hasRoute()) {
                        return processRouteAndPublish(mission, drone, assignment.routeId());
                    } else {
                        log.warn("⚠️ Drone {} has no route assigned. Sending basic execution command.",
                                drone.vehicleId());
                        return publishBasicExecutionCommand(mission, drone);
                    }
                });
    }

    /**
     * Procesa la ruta: busca, extrae waypoints, publica comando
     */
    private CompletableFuture<Void> processRouteAndPublish(
            Mission mission,
            Drone drone,
            String routeId
    ) {
        return routeRepository.findById(routeId)
                .thenCompose(routeOpt -> {
                    if (routeOpt.isEmpty()) {
                        log.error("❌ Route not found: {}", routeId);
                        throw new RouteNotFoundException(
                                "Route not found with id: " + routeId);
                    }

                    KmlRoute route = routeOpt.get();

                    // Validar y extraer waypoints
                    if (!route.hasGeoJson()) {
                        log.error("❌ Route {} does not have GeoJSON content", route.id());
                        throw new InvalidRouteException(
                                "Route " + route.id() + " does not have GeoJSON content");
                    }

                    List<Waypoint> waypoints;
                    try {
                        waypoints = geoJsonParserService.extractWaypoints(route.geojson());
                        log.info("✅ Extracted {} waypoints from route: {} for drone: {}",
                                waypoints.size(), route.id(), drone.vehicleId());
                    } catch (GeoJsonParserService.GeoJsonParseException e) {
                        log.error("❌ Failed to parse GeoJSON for route: {}", route.id(), e);
                        throw new RouteParsingException(
                                "Failed to parse GeoJSON for route: " + route.id(), e);
                    }

                    // Publicar comando con waypoints
                    return publishExecutionCommand(mission, drone, waypoints);
                });
    }

    /**
     * Publica comando de ejecución con waypoints
     */
    private CompletableFuture<Void> publishExecutionCommand(
            Mission mission,
            Drone drone,
            List<Waypoint> waypoints
    ) {
        MissionExecutionCommand command = MissionExecutionCommand.create(
                drone.vehicleId(),
                mission.id(),
                waypoints
        );

        return executionPublisher.publishExecutionCommand(command)
                .thenAccept(result -> {
                    log.info("✅ Execution command published for drone: {} in mission: {}",
                            drone.vehicleId(), mission.id());
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish execution command for drone: {} in mission: {}",
                            drone.vehicleId(), mission.id(), throwable);
                    throw new CommandPublishException(
                            String.format("Failed to publish execution command for drone: %s in mission: %s",
                                    drone.vehicleId(), mission.id()),
                            throwable
                    );
                });
    }

    /**
     * Publica comando básico sin waypoints (cuando no hay ruta asignada)
     */
    private CompletableFuture<Void> publishBasicExecutionCommand(
            Mission mission,
            Drone drone
    ) {
        // Comando básico sin waypoints (lista vacía o waypoint dummy)
        MissionExecutionCommand command = MissionExecutionCommand.create(
                drone.vehicleId(),
                mission.id(),
                List.of() // Sin waypoints - el sistema de control decidirá qué hacer
        );

        return executionPublisher.publishExecutionCommand(command)
                .thenAccept(result -> {
                    log.info("✅ Basic execution command published for drone: {} in mission: {}",
                            drone.vehicleId(), mission.id());
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish basic command for drone: {} in mission: {}",
                            drone.vehicleId(), mission.id(), throwable);
                    throw new CommandPublishException(
                            String.format("Failed to publish basic command for drone: %s in mission: %s",
                                    drone.vehicleId(), mission.id()),
                            throwable
                    );
                });
    }

    /**
     * Actualiza el estado de la misión a EN_EJECUCION con fecha de inicio
     */
    private CompletableFuture<Mission> updateMissionToInProgress(Mission mission) {
        Mission executedMission = mission.withStarted(LocalDateTime.now());

        return missionRepository.save(executedMission)
                .thenApply(savedMission -> {
                    log.info("✅ Mission state updated to EN_EJECUCION: {} (started at: {})",
                            savedMission.id(), savedMission.startDate());
                    return savedMission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to update mission state to EN_EJECUCION: {}",
                            mission.id(), throwable);
                    throw new MissionStateUpdateException(
                            "Failed to update mission state for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    // ========== Excepciones personalizadas ==========

    public static class MissionNotFoundException extends RuntimeException {
        public MissionNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidMissionStateException extends RuntimeException {
        public InvalidMissionStateException(String message) {
            super(message);
        }
    }

    public static class MissionScheduledForFutureException extends RuntimeException {
        public MissionScheduledForFutureException(String message) {
            super(message);
        }
    }

    public static class NoDronesAssignedException extends RuntimeException {
        public NoDronesAssignedException(String message) {
            super(message);
        }
    }

    public static class DroneNotFoundException extends RuntimeException {
        public DroneNotFoundException(String message) {
            super(message);
        }
    }

    public static class RouteNotFoundException extends RuntimeException {
        public RouteNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidRouteException extends RuntimeException {
        public InvalidRouteException(String message) {
            super(message);
        }
    }

    public static class RouteParsingException extends RuntimeException {
        public RouteParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CommandPublishException extends RuntimeException {
        public CommandPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissionExecutionException extends RuntimeException {
        public MissionExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissionExecutionFlowException extends RuntimeException {
        public MissionExecutionFlowException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DroneAssignmentProcessingException extends RuntimeException {
        public DroneAssignmentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissionStateUpdateException extends RuntimeException {
        public MissionStateUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}