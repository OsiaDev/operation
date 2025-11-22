package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.ports.out.DroneMissionRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMissionMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para misiones de drones
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneMissionPersistenceAdapter implements DroneMissionRepository {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final R2dbcDroneMissionRepository r2dbcRepository;

    @Override
    public CompletableFuture<DroneMission> save(DroneMission mission) {
        return toCompletableFuture(
                Mono.just(mission)
                        .map(DroneMissionMapper.toEntity)
                        .flatMap(r2dbcRepository::save)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneMissionMapper.toDomain)
                        .doOnSuccess(saved ->
                                log.info("✅ Saved mission: {} for drone: {}",
                                        saved.id(), saved.droneId()))
                        .doOnError(error ->
                                log.error("❌ Error saving mission for drone: {}",
                                        mission.droneId(), error)),
                () -> "Failed to save mission for drone: " + mission.droneId()
        );
    }

    @Override
    public CompletableFuture<Optional<DroneMission>> findById(String id) {
        return toCompletableFuture(
                r2dbcRepository.findById(UUID.fromString(id))  // ✅ Convertir String → UUID
                        .timeout(DEFAULT_TIMEOUT)
                        .map(DroneMissionMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty()),
                () -> "Failed to find mission by id: " + id
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