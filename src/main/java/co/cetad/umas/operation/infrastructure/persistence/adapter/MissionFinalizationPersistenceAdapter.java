package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.MissionFinalization;
import co.cetad.umas.operation.domain.ports.out.MissionFinalizationRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.MissionFinalizationMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcMissionFinalizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para finalizaciones de misión usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionFinalizationPersistenceAdapter implements MissionFinalizationRepository {

    private final R2dbcMissionFinalizationRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<MissionFinalization> save(MissionFinalization finalization) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = MissionFinalizationMapper.toEntity.apply(finalization);
                var saved = repository.save(entity);

                log.info("✅ Saved mission finalization: {} for mission: {}",
                        saved.getId(), saved.getMissionId());

                return MissionFinalizationMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving mission finalization for mission: {}",
                        finalization.missionId(), e);
                throw new DatabaseOperationException(
                        "Failed to save mission finalization for mission: " + finalization.missionId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionFinalization>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(MissionFinalizationMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission finalization by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionFinalization>> findByMissionId(String missionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByMissionId(UUID.fromString(missionId))
                        .map(MissionFinalizationMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission finalization by mission id: {}", missionId, e);
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