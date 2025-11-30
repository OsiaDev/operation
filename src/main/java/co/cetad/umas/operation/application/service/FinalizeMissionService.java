package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.Mission;
import co.cetad.umas.operation.domain.model.vo.MissionFinalization;
import co.cetad.umas.operation.domain.ports.in.FinalizeMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.MissionFinalizationRepository;
import co.cetad.umas.operation.domain.ports.out.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para finalizar misiones
 * Coordina la actualización del estado de la misión y la creación del registro de finalización
 *
 * IMPORTANTE: Similar a ApproveMissionService pero para cerrar misiones
 *
 * Flujo:
 * 1. Validar entradas
 * 2. Buscar misión
 * 3. Validar que esté EN_EJECUCION
 * 4. Cambiar estado a FINALIZADA con end_date
 * 5. Crear registro de finalización
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinalizeMissionService implements FinalizeMissionUseCase {

    private final MissionRepository missionRepository;
    private final MissionFinalizationRepository finalizationRepository;

    @Override
    public CompletableFuture<Mission> finalizeMission(
            String missionId,
            String commanderName
    ) {
        log.info("Finalizing mission: {} by commander: {}", missionId, commanderName);

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
                            .thenCompose(validatedMission -> finalizeMissionFlow(validatedMission, commanderName));
                })
                .thenApply(finalizedMission -> {
                    log.info("✅ Mission finalized successfully: {} by commander: {}",
                            finalizedMission.id(), commanderName);
                    return finalizedMission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to finalize mission: {}", missionId, throwable);
                    if (throwable.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) throwable.getCause();
                    }
                    throw new MissionFinalizationException(
                            "Failed to finalize mission: " + missionId, throwable);
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
     * Valida que la misión esté en estado EN_EJECUCION
     */
    private CompletableFuture<Mission> validateMissionState(Mission mission) {
        return CompletableFuture.supplyAsync(() -> {
            if (mission.state() != MissionState.EN_EJECUCION) {
                log.warn("⚠️ Mission {} is not in EN_EJECUCION state. Current state: {}",
                        mission.id(), mission.state());
                throw new InvalidMissionStateException(
                        String.format("Mission %s cannot be finalized. Current state: %s. " +
                                        "Only missions in EN_EJECUCION state can be finalized.",
                                mission.id(), mission.state()));
            }
            return mission;
        });
    }

    /**
     * Flujo de finalización: actualiza el estado y crea el registro
     */
    private CompletableFuture<Mission> finalizeMissionFlow(
            Mission mission,
            String commanderName
    ) {
        // Cambiar estado a FINALIZADA con fecha de finalización
        LocalDateTime endDate = LocalDateTime.now();
        Mission finalizedMission = mission.withEnded(endDate, MissionState.FINALIZADA);

        return missionRepository.save(finalizedMission)
                .thenCompose(savedMission ->
                        createFinalizationRecord(savedMission, commanderName)
                                .thenApply(finalization -> savedMission)
                );
    }

    /**
     * Crea el registro de finalización
     */
    private CompletableFuture<MissionFinalization> createFinalizationRecord(
            Mission mission,
            String commanderName
    ) {
        log.debug("Creating finalization record for mission: {} by commander: {}",
                mission.id(), commanderName);

        MissionFinalization finalization = MissionFinalization.create(mission.id(), commanderName);

        return finalizationRepository.save(finalization)
                .thenApply(savedFinalization -> {
                    log.info("✅ Finalization record created: {} for mission: {}",
                            savedFinalization.id(), mission.id());
                    return savedFinalization;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create finalization record for mission: {}",
                            mission.id(), throwable);
                    throw new FinalizationRecordCreationException(
                            "Failed to create finalization record for mission: " + mission.id(),
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

    public static class MissionFinalizationException extends RuntimeException {
        public MissionFinalizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class FinalizationRecordCreationException extends RuntimeException {
        public FinalizationRecordCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}