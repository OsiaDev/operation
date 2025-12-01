package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.MissionApproval;
import co.cetad.umas.operation.domain.ports.out.MissionApprovalRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.MissionApprovalMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcMissionApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para aprobaciones de misión usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionApprovalPersistenceAdapter implements MissionApprovalRepository {

    private final R2dbcMissionApprovalRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<MissionApproval> save(MissionApproval approval) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = MissionApprovalMapper.toEntity.apply(approval);
                var saved = repository.save(entity);

                log.info("✅ Saved mission approval: {} for mission: {}",
                        saved.getId(), saved.getMissionId());

                return MissionApprovalMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving mission approval for mission: {}",
                        approval.missionId(), e);
                throw new DatabaseOperationException(
                        "Failed to save mission approval for mission: " + approval.missionId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionApproval>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(MissionApprovalMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission approval by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<MissionApproval>> findByMissionId(String missionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByMissionId(UUID.fromString(missionId))
                        .map(MissionApprovalMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission approval by mission id: {}", missionId, e);
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