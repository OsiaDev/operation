package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.dto.ExecutionCommand;
import co.cetad.umas.operation.domain.ports.in.ExecuteCommandUseCase;
import co.cetad.umas.operation.domain.ports.out.CommandExecutionPublisher;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.domain.ports.out.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para ejecutar comandos de drones
 * Coordina:
 * 1. Validación del ID de misión
 * 2. Búsqueda de la misión
 * 3. Búsqueda del dron asociado
 * 4. Publicación del comando a Kafka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutionService implements ExecuteCommandUseCase {

    private final MissionRepository missionRepository;
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

                    var mission = missionOpt.get();
                    log.debug("Found mission: {} for drone: {}", mission.id(), mission.droneId());

                    return droneRepository.findById(mission.droneId())
                            .thenCompose(droneOpt -> {
                                if (droneOpt.isEmpty()) {
                                    log.error("❌ Drone not found: {}", mission.droneId());
                                    throw new DroneNotFoundException(
                                            "Drone not found with id: " + mission.droneId());
                                }

                                var drone = droneOpt.get();
                                log.debug("Found drone vehicleId: {} for mission: {}",
                                        drone.vehicleId(), mission.id());

                                return publishCommand(
                                        drone.vehicleId(),
                                        mission.id(),
                                        commandCode
                                );
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
     * Publica el comando al sistema de mensajería
     */
    private CompletableFuture<Void> publishCommand(
            String vehicleId,
            String missionId,
            String commandCode
    ) {
        var command = ExecutionCommand.create(vehicleId, missionId, commandCode);

        return commandPublisher.publishExecutionCommand(command)
                .thenAccept(result -> {
                    log.info("✅ Command published for vehicleId: {}, missionId: {}, commandCode: {}",
                            vehicleId, missionId, commandCode);
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to publish command for mission: {}",
                            missionId, throwable);
                    throw new CommandPublishException(
                            "Failed to publish command for mission: " + missionId,
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