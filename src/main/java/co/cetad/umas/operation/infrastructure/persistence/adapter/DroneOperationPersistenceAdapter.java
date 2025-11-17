package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.DroneOperation;
import co.cetad.umas.operation.domain.ports.out.DroneOperationRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneOperationMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneOperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para operaciones
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneOperationPersistenceAdapter implements DroneOperationRepository {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final R2dbcDroneOperationRepository r2dbcRepository;

    @Override
    public CompletableFuture<DroneOperation> save(DroneOperation operation) {
        return toCompletableFuture(
                Mono.just(operation)
                        .map(DroneOperationMapper.toEntity)
                        .flatMap(r2dbcRepository::save)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneOperationMapper.toDomain)
                        .doOnSuccess(saved ->
                                log.info("✅ Saved operation: {} for drone: {}",
                                        saved.id(), saved.droneId()))
                        .doOnError(error ->
                                log.error("❌ Error saving operation for drone: {}",
                                        operation.droneId(), error)),
                () -> "Failed to save operation for drone: " + operation.droneId()
        );
    }

    @Override
    public CompletableFuture<Optional<DroneOperation>> findById(String id) {
        return toCompletableFuture(
                r2dbcRepository.findById(id)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneOperationMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty()),
                () -> "Failed to find operation by id: " + id
        );
    }

    private <T> CompletableFuture<T> toCompletableFuture(
            Mono<T> mono,
            ErrorMessageSupplier errorMessageSupplier
    ) {
        return mono.toFuture()
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        String errorMessage = errorMessageSupplier.get();
                        log.error(errorMessage, throwable);
                        throw new DatabaseOperationException(errorMessage, throwable);
                    }
                    return result;
                });
    }

    @FunctionalInterface
    private interface ErrorMessageSupplier {
        String get();
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}