package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.dto.MissionExecutionCommand;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.model.vo.KmlRoute;
import co.cetad.umas.operation.domain.model.vo.Waypoint;
import co.cetad.umas.operation.domain.ports.in.ExecuteMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.domain.ports.out.DroneMissionRepository;
import co.cetad.umas.operation.domain.ports.out.KmlRouteRepository;
import co.cetad.umas.operation.domain.ports.out.MissionExecutionPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para ejecutar misiones de drones
 * Coordina:
 * 1. Validación de la misión
 * 2. Búsqueda y extracción de la ruta con waypoints
 * 3. Publicación del comando de ejecución a Kafka
 * 4. Actualización del estado de la misión a EN_EJECUCION
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteMissionService implements ExecuteMissionUseCase {

    private final DroneMissionRepository missionRepository;
    private final KmlRouteRepository routeRepository;
    private final DroneRepository droneRepository;
    private final GeoJsonParserService geoJsonParserService;
    private final MissionExecutionPublisher executionPublisher;

    @Override
    public CompletableFuture<DroneMission> executeMission(
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

                    DroneMission mission = missionOpt.get();
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
     * Valida que la misión esté en estado APROBADA y tenga una ruta asignada
     */
    private CompletableFuture<DroneMission> validateMissionState(DroneMission mission) {
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
                        mission.id(), mission.startDate());
                throw new MissionScheduledForFutureException(
                        String.format("Mission %s is scheduled for future date: %s. " +
                                        "Cannot execute missions scheduled for the future.",
                                mission.id(), mission.startDate()));
            }

            // Validar que la misión tenga una ruta asignada
            if (!mission.hasRoute()) {
                log.error("❌ Mission {} does not have a route assigned", mission.id());
                throw new MissingRouteException(
                        String.format("Mission %s cannot be executed. " +
                                        "No route assigned to this mission.",
                                mission.id()));
            }

            return mission;
        });
    }

    /**
     * Flujo de ejecución completo:
     * 1. Obtener el vehicleId del dron asociado
     * 2. Buscar la ruta KML
     * 3. Extraer waypoints del GeoJSON
     * 4. Publicar comando de ejecución a Kafka
     * 5. Actualizar estado de la misión a EN_EJECUCION
     */
    private CompletableFuture<DroneMission> executeMissionFlow(DroneMission mission) {
        log.info("🚀 Starting execution flow for mission: {}", mission.id());

        // Paso 1: Obtener vehicleId del dron
        return droneRepository.findById(mission.droneId())
                .thenCompose(droneOpt -> {
                    if (droneOpt.isEmpty()) {
                        log.error("❌ Drone not found: {}", mission.droneId());
                        throw new DroneNotFoundException(
                                "Drone not found with id: " + mission.droneId());
                    }

                    String vehicleId = droneOpt.get().vehicleId();
                    log.debug("Found drone vehicleId: {} for mission: {}",
                            vehicleId, mission.id());

                    // Paso 2: Buscar ruta y extraer waypoints
                    return routeRepository.findById(mission.routeId())
                            .thenCompose(routeOpt -> {
                                if (routeOpt.isEmpty()) {
                                    log.error("❌ Route not found: {}", mission.routeId());
                                    throw new RouteNotFoundException(
                                            "Route not found with id: " + mission.routeId());
                                }

                                KmlRoute route = routeOpt.get();
                                return processRouteAndExecute(mission, vehicleId, route);
                            });
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
     * Procesa la ruta, extrae waypoints, publica comando y actualiza misión
     */
    private CompletableFuture<DroneMission> processRouteAndExecute(
            DroneMission mission,
            String vehicleId,
            KmlRoute route
    ) {
        // Paso 3: Validar y extraer waypoints del GeoJSON
        if (!route.hasGeoJson()) {
            log.error("❌ Route {} does not have GeoJSON content", route.id());
            throw new InvalidRouteException(
                    "Route " + route.id() + " does not have GeoJSON content");
        }

        List<Waypoint> waypoints;
        try {
            waypoints = geoJsonParserService.extractWaypoints(route.geojson());
            log.info("✅ Extracted {} waypoints from route: {}", waypoints.size(), route.id());
        } catch (GeoJsonParserService.GeoJsonParseException e) {
            log.error("❌ Failed to parse GeoJSON for route: {}", route.id(), e);
            throw new RouteParsingException(
                    "Failed to parse GeoJSON for route: " + route.id(), e);
        }

        // Paso 4: Crear y publicar comando de ejecución
        MissionExecutionCommand command = MissionExecutionCommand.create(
                vehicleId,
                mission.id(),
                waypoints
        );

        return executionPublisher.publishExecutionCommand(command)
                .thenCompose(published -> {
                    log.info("✅ Execution command published for mission: {}", mission.id());

                    // Paso 5: Actualizar estado de la misión a EN_EJECUCION
                    return updateMissionState(mission);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish execution command or update mission state: {}",
                            mission.id(), throwable);
                    throw new CommandPublishException(
                            "Failed to publish execution command for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    /**
     * Actualiza el estado de la misión a EN_EJECUCION
     */
    private CompletableFuture<DroneMission> updateMissionState(DroneMission mission) {
        DroneMission executedMission = mission.withState(MissionState.EN_EJECUCION);

        return missionRepository.save(executedMission)
                .thenApply(savedMission -> {
                    log.info("✅ Mission state updated to EN_EJECUCION: {}", savedMission.id());
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

    public static class MissingRouteException extends RuntimeException {
        public MissingRouteException(String message) {
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

    public static class MissionStateUpdateException extends RuntimeException {
        public MissionStateUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}