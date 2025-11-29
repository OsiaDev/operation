package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.dto.ExecutionCommand;
import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.model.vo.Mission;
import co.cetad.umas.operation.domain.ports.in.ExecuteCommandUseCase;
import co.cetad.umas.operation.domain.ports.out.CommandExecutionPublisher;
import co.cetad.umas.operation.domain.ports.out.DroneMissionAssignmentRepository;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.domain.ports.out.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para ejecutar comandos de drones
 *
 * REFACTORIZACIÓN: Ahora coordina:
 * 1. Validación del ID de misión
 * 2. Búsqueda de la misión
 * 3. Búsqueda de TODOS los drones asignados a la misión
 * 4. Publicación del comando a Kafka para CADA dron
 *
 * IMPORTANTE: Un comando se envía a TODOS los drones asignados a la misión
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutionService implements ExecuteCommandUseCase {

    private final MissionRepository missionRepository;
    private final DroneMissionAssignmentRepository assignmentRepository;
    private final DroneRepository droneRepository;
    private final CommandExecutionPublisher commandPublisher;

    @Override
    public CompletableFuture<Void> executeCommand(String missionId, String commandCode) {
        log.info("Executing command: {} for mission: {}", commandCode, missionId);

        return validateInputs(missionId, commandCode)
                .thenCompose(validated -> missionRepository.findById(missionId))
                .thenCompose(missionOpt -> {
                    if (missionOpt.isEmpty()) {
                        log.error("❌ Mission not found: {}", missionId);
                        throw new MissionNotFoundException(
                                "Mission not found with id: " + missionId);
                    }

                    Mission mission = missionOpt.get();
                    log.debug("Found mission: {}", mission.id());

                    // Buscar TODOS los drones asignados a esta misión
                    return assignmentRepository.findByMissionId(mission.id())
                            .thenCompose(assignments -> {
                                if (assignments.isEmpty()) {
                                    log.error("❌ No drones assigned to mission: {}", mission.id());
                                    throw new NoDronesAssignedException(
                                            "No drones assigned to mission: " + mission.id());
                                }

                                log.info("Found {} drone(s) assigned to mission: {}",
                                        assignments.size(), mission.id());

                                // Publicar comando para CADA dron asignado
                                return publishCommandToAllDrones(mission, assignments, commandCode);
                            });
                })
                .thenRun(() -> {
                    log.info("✅ Command {} executed successfully for mission: {}",
                            commandCode, missionId);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to execute command {} for mission: {}",
                            commandCode, missionId, throwable);
                    if (throwable.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) throwable.getCause();
                    }
                    throw new CommandExecutionException(
                            String.format("Failed to execute command %s for mission: %s",
                                    commandCode, missionId),
                            throwable
                    );
                });
    }

    /**
     * Valida las entradas del caso de uso
     */
    private CompletableFuture<Void> validateInputs(String missionId, String commandCode) {
        return CompletableFuture.runAsync(() -> {
            if (missionId == null || missionId.isBlank()) {
                throw new IllegalArgumentException("Mission ID cannot be null or empty");
            }
            if (commandCode == null || commandCode.isBlank()) {
                throw new IllegalArgumentException("Command code cannot be null or empty");
            }
        });
    }

    /**
     * Publica el comando a TODOS los drones asignados a la misión en paralelo
     */
    private CompletableFuture<Void> publishCommandToAllDrones(
            Mission mission,
            List<DroneMissionAssignment> assignments,
            String commandCode
    ) {
        log.info("Publishing command '{}' to {} drone(s) for mission: {}",
                commandCode, assignments.size(), mission.id());

        // Crear futures para publicar comando a cada dron
        List<CompletableFuture<Void>> publishFutures = assignments.stream()
                .map(assignment -> publishCommandForDrone(mission, assignment, commandCode))
                .toList();

        // Esperar a que todos los comandos se publiquen
        return CompletableFuture.allOf(
                        publishFutures.toArray(new CompletableFuture[0])
                )
                .thenRun(() -> {
                    log.info("✅ Command '{}' published to all {} drone(s) for mission: {}",
                            commandCode, assignments.size(), mission.id());
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish command to all drones for mission: {}",
                            mission.id(), throwable);
                    throw new CommandPublishException(
                            "Failed to publish command to all drones for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    /**
     * Publica el comando para un dron específico
     */
    private CompletableFuture<Void> publishCommandForDrone(
            Mission mission,
            DroneMissionAssignment assignment,
            String commandCode
    ) {
        log.debug("Publishing command '{}' for drone: {} in mission: {}",
                commandCode, assignment.droneId(), mission.id());

        return droneRepository.findById(assignment.droneId())
                .thenCompose(droneOpt -> {
                    if (droneOpt.isEmpty()) {
                        log.error("❌ Drone not found: {}", assignment.droneId());
                        throw new DroneNotFoundException(
                                "Drone not found with id: " + assignment.droneId());
                    }

                    Drone drone = droneOpt.get();
                    log.debug("Found drone vehicleId: {} for assignment", drone.vehicleId());

                    return publishCommand(
                            drone.vehicleId(),
                            mission.id(),
                            commandCode
                    );
                })
                .thenRun(() -> {
                    log.debug("✅ Command '{}' published for drone: {}",
                            commandCode, assignment.droneId());
                });
    }

    /**
     * Publica el comando al sistema de mensajería (Kafka)
     */
    private CompletableFuture<Void> publishCommand(
            String vehicleId,
            String missionId,
            String commandCode
    ) {
        ExecutionCommand command = ExecutionCommand.create(vehicleId, missionId, commandCode);

        return commandPublisher.publishExecutionCommand(command)
                .thenAccept(result -> {
                    log.debug("✅ Command published for vehicleId: {}, missionId: {}, commandCode: {}",
                            vehicleId, missionId, commandCode);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish command for vehicleId: {} in mission: {}",
                            vehicleId, missionId, throwable);
                    throw new CommandPublishException(
                            String.format("Failed to publish command for vehicleId: %s in mission: %s",
                                    vehicleId, missionId),
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

    public static class CommandExecutionException extends RuntimeException {
        public CommandExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class CommandPublishException extends RuntimeException {
        public CommandPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}