package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.model.vo.MissionOrder;
import co.cetad.umas.operation.domain.ports.in.CreateDroneMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionRepository;
import co.cetad.umas.operation.domain.ports.out.MissionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para misiones de drones
 * Coordina la creación de misión y su orden asociada
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneMissionService implements CreateDroneMissionUseCase {

    private final DroneMissionRepository missionRepository;
    private final MissionOrderRepository orderRepository;

    @Override
    public CompletableFuture<DroneMission> createMission(
            DroneMission mission,
            String commanderName
    ) {
        log.info("Creating mission for drone: {} with commander: {}",
                mission.droneId(), commanderName);

        return CompletableFuture
                .supplyAsync(() -> validateMission(mission, commanderName))
                .thenCompose(validatedMission ->
                        missionRepository.save(validatedMission)
                                .thenCompose(savedMission ->
                                        createMissionOrder(savedMission, commanderName)
                                                .thenApply(order -> savedMission)
                                )
                )
                .thenApply(savedMission -> {
                    log.info("✅ Mission created successfully: {} for drone: {}",
                            savedMission.id(), savedMission.droneId());
                    return savedMission;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create mission for drone: {}",
                            mission.droneId(), throwable);
                    throw new MissionCreationException(
                            "Failed to create mission for drone: " + mission.droneId(),
                            throwable
                    );
                });
    }

    /**
     * Valida la misión antes de guardarla
     */
    private DroneMission validateMission(DroneMission mission, String commanderName) {
        log.debug("Validating mission for drone: {}", mission.droneId());

        if (mission.id() == null || mission.id().isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be null or empty");
        }
        if (mission.droneId() == null || mission.droneId().isBlank()) {
            throw new IllegalArgumentException("Drone ID cannot be null or empty");
        }
        if (mission.operatorId() == null || mission.operatorId().isBlank()) {
            throw new IllegalArgumentException("Operator ID cannot be null or empty");
        }
        if (mission.startDate() == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (commanderName == null || commanderName.isBlank()) {
            throw new IllegalArgumentException("Commander name cannot be null or empty");
        }

        log.debug("✅ Mission validation successful for drone: {}", mission.droneId());
        return mission;
    }

    /**
     * Crea la orden de misión asociada
     */
    private CompletableFuture<MissionOrder> createMissionOrder(
            DroneMission mission,
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

}