package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.ports.in.ExecuteMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para ejecutar misiones de drones
 * Coordina la transición del estado de APROBADA a EN_EJECUCION
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteMissionService implements ExecuteMissionUseCase {

    private final DroneMissionRepository missionRepository;

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
                            .thenCompose(validatedMission -> executeMissionFlow(validatedMission));
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

            return mission;
        });
    }

    /**
     * Flujo de ejecución: actualiza el estado a EN_EJECUCION
     */
    private CompletableFuture<DroneMission> executeMissionFlow(DroneMission mission) {
        // Cambiar estado a EN_EJECUCION
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

    public static class MissionExecutionException extends RuntimeException {
        public MissionExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissionStateUpdateException extends RuntimeException {
        public MissionStateUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}