package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.model.vo.MissionApproval;
import co.cetad.umas.operation.domain.ports.in.ApproveMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionRepository;
import co.cetad.umas.operation.domain.ports.out.MissionApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para aprobar misiones de drones
 * Coordina la actualización del estado de la misión y la creación del registro de aprobación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveMissionService implements ApproveMissionUseCase {

    private final DroneMissionRepository missionRepository;
    private final MissionApprovalRepository approvalRepository;

    @Override
    public CompletableFuture<DroneMission> approveMission(
            String missionId,
            String commanderName
    ) {
        log.info("Approving mission: {} by commander: {}", missionId, commanderName);

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
                            .thenCompose(validatedMission -> approveMissionFlow(validatedMission, commanderName));
                })
                .thenApply(approvedMission -> {
                    log.info("✅ Mission approved successfully: {} by commander: {}",
                            approvedMission.id(), commanderName);
                    return approvedMission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to approve mission: {}", missionId, throwable);
                    if (throwable.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) throwable.getCause();
                    }
                    throw new MissionApprovalException(
                            "Failed to approve mission: " + missionId, throwable);
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
     * Valida que la misión esté en estado PENDIENTE_APROBACION
     */
    private CompletableFuture<DroneMission> validateMissionState(DroneMission mission) {
        return CompletableFuture.supplyAsync(() -> {
            if (mission.state() != MissionState.PENDIENTE_APROBACION) {
                log.warn("⚠️ Mission {} is not in PENDIENTE_APROBACION state. Current state: {}",
                        mission.id(), mission.state());
                throw new InvalidMissionStateException(
                        String.format("Mission %s cannot be approved. Current state: %s. " +
                                        "Only missions in PENDIENTE_APROBACION state can be approved.",
                                mission.id(), mission.state()));
            }
            return mission;
        });
    }

    /**
     * Flujo de aprobación: actualiza el estado y crea el registro
     */
    private CompletableFuture<DroneMission> approveMissionFlow(
            DroneMission mission,
            String commanderName
    ) {
        // Cambiar estado a APROBADA
        DroneMission approvedMission = mission.withState(MissionState.APROBADA);

        return missionRepository.save(approvedMission)
                .thenCompose(savedMission ->
                        createApprovalRecord(savedMission, commanderName)
                                .thenApply(approval -> savedMission)
                );
    }

    /**
     * Crea el registro de aprobación
     */
    private CompletableFuture<MissionApproval> createApprovalRecord(
            DroneMission mission,
            String commanderName
    ) {
        log.debug("Creating approval record for mission: {} by commander: {}",
                mission.id(), commanderName);

        MissionApproval approval = MissionApproval.create(mission.id(), commanderName);

        return approvalRepository.save(approval)
                .thenApply(savedApproval -> {
                    log.info("✅ Approval record created: {} for mission: {}",
                            savedApproval.id(), mission.id());
                    return savedApproval;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create approval record for mission: {}",
                            mission.id(), throwable);
                    throw new ApprovalRecordCreationException(
                            "Failed to create approval record for mission: " + mission.id(),
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

    public static class MissionApprovalException extends RuntimeException {
        public MissionApprovalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ApprovalRecordCreationException extends RuntimeException {
        public ApprovalRecordCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}