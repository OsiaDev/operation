package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.ports.out.DroneMissionRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMissionMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para misiones de drones usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneMissionPersistenceAdapter implements DroneMissionRepository {

    private final R2dbcDroneMissionRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<DroneMission> save(DroneMission mission) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = DroneMissionMapper.toEntity.apply(mission);
                var saved = repository.save(entity);

                log.info("✅ Saved mission: {} for drone: {}",
                        saved.getId(), saved.getDroneId());

                return DroneMissionMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving mission for drone: {}", mission.droneId(), e);
                throw new DatabaseOperationException(
                        "Failed to save mission for drone: " + mission.droneId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<DroneMission>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(DroneMissionMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding mission by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<DroneMission>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findAll()
                        .stream()
                        .map(DroneMissionMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding all missions", e);
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