package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.MissionExecution;
import co.cetad.umas.operation.domain.ports.out.MissionExecutionRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.MissionExecutionMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcMissionExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para ejecuciones de misión usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionExecutionPersistenceAdapter implements MissionExecutionRepository {

    private final R2dbcMissionExecutionRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<MissionExecution> save(MissionExecution execution) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = MissionExecutionMapper.toEntity.apply(execution);
                var saved = repository.save(entity);

                log.info("✅ Saved mission execution: {} for mission: {}",
                        saved.getId(), saved.getMissionId());

                return MissionExecutionMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving mission execution for mission: {}",
                        execution.missionId(), e);
                throw new DatabaseOperationException(
                        "Failed to save mission execution for mission: " + execution.missionId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionExecution>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(MissionExecutionMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission execution by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionExecution>> findByMissionId(String missionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByMissionId(UUID.fromString(missionId))
                        .map(MissionExecutionMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission execution by mission id: {}", missionId, e);
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