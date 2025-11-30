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
 * 3. Para cada dron: buscar ruta, extraer waypoints, incluir routeId
 * 4. Construir lista de DroneExecution (vehicleId + routeId + waypoints)
 * 5. Publicar UN SOLO mensaje con todos los drones
 * 6. Actualización del estado de la misión a EN_EJECUCION
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
     * 2. Para cada dron: obtener vehicleId, buscar ruta, extraer waypoints, incluir routeId
     * 3. Construir lista de DroneExecution con vehicleId, routeId y waypoints
     * 4. Publicar UN SOLO comando con todos los drones
     * 5. Actualizar estado de la misión a EN_EJECUCION con start_date
     *
     * ACTUALIZACIÓN: Ahora incluye routeId en cada DroneExecution
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

                    // Paso 2: Procesar TODOS los drones y construir lista de DroneExecution
                    return buildDroneExecutionList(mission, assignments);
                })
                .thenCompose(droneExecutions -> {
                    // Paso 3: Publicar UN SOLO comando con todos los drones
                    return publishMissionExecutionCommand(mission, droneExecutions);
                })
                .thenCompose(processedMission -> {
                    // Paso 4: Actualizar estado de la misión a EN_EJECUCION
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
     * Construye la lista de DroneExecution procesando todas las asignaciones en paralelo
     * Cada DroneExecution contiene el vehicleId, routeId y waypoints del dron
     *
     * OPTIMIZACIÓN: Procesa todos los drones en paralelo
     * ACTUALIZACIÓN: Ahora incluye routeId en cada DroneExecution
     *
     * @param mission Misión a ejecutar
     * @param assignments Lista de asignaciones de drones
     * @return CompletableFuture con lista de DroneExecution
     */
    private CompletableFuture<List<MissionExecutionCommand.DroneExecution>> buildDroneExecutionList(
            Mission mission,
            List<DroneMissionAssignment> assignments
    ) {
        log.info("Building DroneExecution list for {} drone(s) in mission: {}",
                assignments.size(), mission.id());

        // Crear futures para procesar cada asignación en paralelo
        List<CompletableFuture<MissionExecutionCommand.DroneExecution>> executionFutures =
                assignments.stream()
                        .map(this::buildSingleDroneExecution)
                        .toList();

        // Esperar a que todas las ejecuciones se construyan
        return CompletableFuture.allOf(
                        executionFutures.toArray(new CompletableFuture[0])
                )
                .thenApply(v -> {
                    List<MissionExecutionCommand.DroneExecution> droneExecutions =
                            executionFutures.stream()
                                    .map(CompletableFuture::join)
                                    .toList();

                    log.info("✅ Built {} DroneExecution(s) for mission: {}",
                            droneExecutions.size(), mission.id());

                    return droneExecutions;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to build DroneExecution list for mission: {}",
                            mission.id(), throwable);
                    throw new DroneExecutionBuildException(
                            "Failed to build DroneExecution list for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    /**
     * Construye un DroneExecution para una asignación específica:
     * 1. Buscar dron para obtener vehicleId
     * 2. Si tiene ruta: buscar ruta, extraer waypoints, incluir routeId
     * 3. Crear DroneExecution con vehicleId, routeId y waypoints
     *
     * @param assignment Asignación de dron a procesar
     * @return CompletableFuture con DroneExecution
     */
    private CompletableFuture<MissionExecutionCommand.DroneExecution> buildSingleDroneExecution(
            DroneMissionAssignment assignment
    ) {
        log.debug("Building DroneExecution for assignment: {}", assignment.id());

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

                    // Paso 2: Si tiene ruta, procesarla; si no, retornar sin waypoints
                    if (assignment.hasRoute()) {
                        return loadWaypointsForDrone(drone, assignment.routeId());
                    } else {
                        log.warn("⚠️ Drone {} has no route assigned. Creating DroneExecution without waypoints.",
                                drone.vehicleId());
                        return CompletableFuture.completedFuture(
                                MissionExecutionCommand.DroneExecution.createWithoutRoute(
                                        drone.vehicleId()
                                )
                        );
                    }
                });
    }

    /**
     * Carga los waypoints de la ruta asignada a un dron
     *
     * ACTUALIZACIÓN: Ahora incluye el routeId en el DroneExecution
     *
     * @param drone Dron al que se le asignará la ruta
     * @param routeId ID de la ruta a cargar
     * @return CompletableFuture con DroneExecution que incluye vehicleId, routeId y waypoints
     */
    private CompletableFuture<MissionExecutionCommand.DroneExecution> loadWaypointsForDrone(
            Drone drone,
            String routeId
    ) {
        return routeRepository.findById(routeId)
                .thenApply(routeOpt -> {
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

                    // Crear DroneExecution con vehicleId, routeId y waypoints
                    return MissionExecutionCommand.DroneExecution.create(
                            drone.vehicleId(),
                            routeId,
                            waypoints
                    );
                });
    }

    /**
     * Publica UN SOLO comando de ejecución con TODOS los drones
     *
     * REFACTORIZACIÓN: Ahora envía un solo mensaje en lugar de uno por dron
     *
     * @param mission Misión a ejecutar
     * @param droneExecutions Lista de DroneExecution con vehicleId, routeId y waypoints
     * @return CompletableFuture con la misión
     */
    private CompletableFuture<Mission> publishMissionExecutionCommand(
            Mission mission,
            List<MissionExecutionCommand.DroneExecution> droneExecutions
    ) {
        MissionExecutionCommand command = MissionExecutionCommand.create(
                mission.id(),
                droneExecutions
        );

        return executionPublisher.publishExecutionCommand(command)
                .thenApply(result -> {
                    log.info("✅ Execution command published for mission: {} with {} drone(s)",
                            mission.id(), droneExecutions.size());
                    return mission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish execution command for mission: {}",
                            mission.id(), throwable);
                    throw new CommandPublishException(
                            String.format("Failed to publish execution command for mission: %s",
                                    mission.id()),
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

    public static class DroneExecutionBuildException extends RuntimeException {
        public DroneExecutionBuildException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissionStateUpdateException extends RuntimeException {
        public MissionStateUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}