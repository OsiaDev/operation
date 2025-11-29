package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.Mission;
import co.cetad.umas.operation.domain.ports.out.MissionRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.MissionMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para misiones usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission independiente de drones
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionPersistenceAdapter implements MissionRepository {

    private final R2dbcMissionRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<Mission> save(Mission mission) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = MissionMapper.toEntity.apply(mission);
                var saved = repository.save(entity);

                log.info("✅ Saved mission: {} with estimated date: {}",
                        saved.getId(), saved.getEstimatedDate());

                return MissionMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving mission: {}", mission.id(), e);
                throw new DatabaseOperationException(
                        "Failed to save mission: " + mission.id(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Mission>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(MissionMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findAll()
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding all missions", e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findByMissionType(MissionOrigin missionType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Querying missions by type: {}", missionType);

                List<Mission> missions = repository.findByMissionType(missionType)
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();

                log.info("✅ Found {} missions with type: {}", missions.size(), missionType);
                return missions;
            } catch (Exception e) {
                log.error("❌ Error finding missions by type: {}", missionType, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findByState(MissionState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByState(state)
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding missions by state: {}", state, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findPendingApproval() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findPendingApproval()
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding pending approval missions", e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findByEstimatedDate(LocalDateTime date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByEstimatedDate(date)
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding missions by estimated date: {}", date, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findInProgress() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findInProgress()
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding in-progress missions", e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<Mission>> findFinished() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findFinished()
                        .stream()
                        .map(MissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding finished missions", e);
                return List.of();
            }
        });
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}