package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.vo.DroneOperation;
import co.cetad.umas.operation.domain.ports.in.CreateDroneOperationUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio de aplicación para operaciones de drones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneOperationService implements CreateDroneOperationUseCase {

    private final DroneOperationRepository operationRepository;

    @Override
    public CompletableFuture<DroneOperation> createOperation(DroneOperation operation) {
        log.info("Creating operation for drone: {} with start date: {}",
                operation.droneId(), operation.startDate());

        return CompletableFuture
                .supplyAsync(() -> validateOperation(operation))
                .thenCompose(operationRepository::save)
                .thenApply(savedOperation -> {
                    log.info("✅ Operation created successfully: {} for drone: {}",
                            savedOperation.id(), savedOperation.droneId());
                    return savedOperation;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create operation for drone: {}",
                            operation.droneId(), throwable);
                    throw new OperationCreationException(
                            "Failed to create operation for drone: " + operation.droneId(),
                            throwable
                    );
                });
    }

    private DroneOperation validateOperation(DroneOperation operation) {
        log.debug("Validating operation for drone: {}", operation.droneId());

        if (operation.id() == null || operation.id().isBlank()) {
            throw new IllegalArgumentException("Operation ID cannot be null or empty");
        }
        if (operation.droneId() == null || operation.droneId().isBlank()) {
            throw new IllegalArgumentException("Drone ID cannot be null or empty");
        }
        if (operation.startDate() == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }

        log.debug("✅ Operation validation successful for drone: {}", operation.droneId());
        return operation;
    }

    public static class OperationCreationException extends RuntimeException {
        public OperationCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}