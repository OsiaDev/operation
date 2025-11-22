package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.MissionOrder;
import co.cetad.umas.operation.domain.ports.out.MissionOrderRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.MissionOrderMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcMissionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para órdenes de misión usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionOrderPersistenceAdapter implements MissionOrderRepository {

    private final R2dbcMissionOrderRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<MissionOrder> save(MissionOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = MissionOrderMapper.toEntity.apply(order);
                var saved = repository.save(entity);

                log.info("✅ Saved mission order: {} for mission: {}",
                        saved.getId(), saved.getMissionId());

                return MissionOrderMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving mission order for mission: {}",
                        order.missionId(), e);
                throw new DatabaseOperationException(
                        "Failed to save mission order for mission: " + order.missionId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionOrder>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(MissionOrderMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission order by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}