package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.model.vo.Mission;
import co.cetad.umas.operation.domain.model.vo.MissionOrder;
import co.cetad.umas.operation.domain.ports.in.CreateMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionAssignmentRepository;
import co.cetad.umas.operation.domain.ports.out.MissionOrderRepository;
import co.cetad.umas.operation.domain.ports.out.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para crear misiones con drones asignados
 *
 * REFACTORIZACIÓN: Ahora coordina la creación de:
 * 1. Mission (misión general)
 * 2. MissionOrder (orden de misión para auditoría)
 * 3. DroneMissionAssignment(s) (asignaciones de drones a la misión)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateMissionService implements CreateMissionUseCase {

    private final MissionRepository missionRepository;
    private final MissionOrderRepository orderRepository;
    private final DroneMissionAssignmentRepository assignmentRepository;

    @Override
    public CompletableFuture<Mission> createMission(
            Mission mission,
            String commanderName,
            List<DroneMissionAssignment> droneAssignments
    ) {
        log.info("Creating mission with {} drone(s) assigned by commander: {}",
                droneAssignments.size(), commanderName);

        return CompletableFuture
                .supplyAsync(() -> validateMissionCreation(mission, commanderName, droneAssignments))
                .thenCompose(validatedMission ->
                        // Paso 1: Guardar la misión
                        missionRepository.save(validatedMission)
                                .thenCompose(savedMission ->
                                        // Paso 2: Crear orden de misión
                                        createMissionOrder(savedMission, commanderName)
                                                .thenCompose(order ->
                                                        // Paso 3: Asignar drones
                                                        assignDronesToMission(savedMission, droneAssignments)
                                                                .thenApply(assignments -> savedMission)
                                                )
                                )
                )
                .thenApply(savedMission -> {
                    log.info("✅ Mission created successfully: {} with {} drone(s) assigned",
                            savedMission.id(), droneAssignments.size());
                    return savedMission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create mission", throwable);
                    throw new MissionCreationException(
                            "Failed to create mission with drone assignments",
                            throwable
                    );
                });
    }

    /**
     * Valida la misión antes de guardarla
     */
    private Mission validateMissionCreation(
            Mission mission,
            String commanderName,
            List<DroneMissionAssignment> droneAssignments
    ) {
        log.debug("Validating mission creation");

        if (mission.id() == null) {
            throw new IllegalArgumentException("Mission ID cannot be null");
        }
        if (mission.operatorId() == null) {
            throw new IllegalArgumentException("Operator ID cannot be null");
        }
        if (mission.estimatedDate() == null) {
            throw new IllegalArgumentException("Estimated date cannot be null");
        }
        if (commanderName == null || commanderName.isBlank()) {
            throw new IllegalArgumentException("Commander name cannot be null or empty");
        }
        if (droneAssignments == null || droneAssignments.isEmpty()) {
            throw new IllegalArgumentException("At least one drone must be assigned to the mission");
        }

        // Validar que todas las asignaciones sean para la misma misión
        boolean allMatchMissionId = droneAssignments.stream()
                .allMatch(assignment -> assignment.missionId().equals(mission.id()));

        if (!allMatchMissionId) {
            throw new IllegalArgumentException(
                    "All drone assignments must belong to the same mission");
        }

        log.debug("✅ Mission validation successful");
        return mission;
    }

    /**
     * Crea la orden de misión asociada
     */
    private CompletableFuture<MissionOrder> createMissionOrder(
            Mission mission,
            String commanderName
    ) {
        log.debug("Creating mission order for mission: {} by commander: {}",
                mission.id(), commanderName);

        MissionOrder order = MissionOrder.create(mission.id(), commanderName);

        return orderRepository.save(order)
                .thenApply(savedOrder -> {
                    log.info("✅ Mission order created: {} for mission: {}",
                            savedOrder.id(), mission.id());
                    return savedOrder;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create mission order for mission: {}",
                            mission.id(), throwable);
                    throw new MissionOrderCreationException(
                            "Failed to create mission order for mission: " + mission.id(),
                            throwable
                    );
                });
    }

    /**
     * Asigna los drones a la misión de forma asíncrona
     */
    private CompletableFuture<List<DroneMissionAssignment>> assignDronesToMission(
            Mission mission,
            List<DroneMissionAssignment> droneAssignments
    ) {
        log.debug("Assigning {} drone(s) to mission: {}",
                droneAssignments.size(), mission.id());

        // Guardar todas las asignaciones en paralelo
        List<CompletableFuture<DroneMissionAssignment>> assignmentFutures = droneAssignments.stream()
                .map(assignment -> assignmentRepository.save(assignment)
                        .exceptionally(throwable -> {
                            log.error("❌ Failed to assign drone {} to mission {}",
                                    assignment.droneId(), mission.id(), throwable);
                            throw new DroneAssignmentException(
                                    String.format("Failed to assign drone %s to mission %s",
                                            assignment.droneId(), mission.id()),
                                    throwable
                            );
                        })
                )
                .toList();

        // Esperar a que todas las asignaciones se completen
        return CompletableFuture.allOf(
                        assignmentFutures.toArray(new CompletableFuture[0])
                )
                .thenApply(v -> assignmentFutures.stream()
                        .map(CompletableFuture::join)
                        .toList()
                )
                .thenApply(assignments -> {
                    log.info("✅ Successfully assigned {} drone(s) to mission: {}",
                            assignments.size(), mission.id());
                    return assignments;
                });
    }

    // ========== Excepciones personalizadas ==========

    public static class MissionCreationException extends RuntimeException {
        public MissionCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissionOrderCreationException extends RuntimeException {
        public MissionOrderCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DroneAssignmentException extends RuntimeException {
        public DroneAssignmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}