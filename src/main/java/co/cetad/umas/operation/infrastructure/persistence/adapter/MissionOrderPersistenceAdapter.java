package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.MissionOrder;
import co.cetad.umas.operation.domain.ports.out.MissionOrderRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.MissionOrderMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcMissionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para órdenes de misión
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionOrderPersistenceAdapter implements MissionOrderRepository {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private final R2dbcMissionOrderRepository r2dbcRepository;

    @Override
    public CompletableFuture<MissionOrder> save(MissionOrder order) {
        return toCompletableFuture(
                Mono.just(order)
                        .map(MissionOrderMapper.toEntity)
                        .flatMap(r2dbcRepository::save)
                        .timeout(DEFAULT_TIMEOUT)
                        .map(MissionOrderMapper.toDomain)
                        .doOnSuccess(saved ->
                                log.info("✅ Saved mission order: {} for mission: {}",
                                        saved.id(), saved.missionId()))
                        .doOnError(error ->
                                log.error("❌ Error saving mission order for mission: {}",
                                        order.missionId(), error)),
                () -> "Failed to save mission order for mission: " + order.missionId()
        );
    }

    @Override
    public CompletableFuture<Optional<MissionOrder>> findById(String id) {
        return toCompletableFuture(
                r2dbcRepository.findById(UUID.fromString(id))  // ✅ Convertir String → UUID
                        .timeout(DEFAULT_TIMEOUT)
                        .map(MissionOrderMapper.toDomain)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty()),
                () -> "Failed to find mission order by id: " + id
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