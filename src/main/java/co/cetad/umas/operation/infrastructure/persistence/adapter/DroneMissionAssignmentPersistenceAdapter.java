package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.ports.out.DroneMissionAssignmentRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMissionAssignmentMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneMissionAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para asignaciones de drones a misiones usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * REFACTORIZACIÓN: Nuevo adaptador para manejar relación muchos-a-muchos
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneMissionAssignmentPersistenceAdapter
        implements DroneMissionAssignmentRepository {

    private final R2dbcDroneMissionAssignmentRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<DroneMissionAssignment> save(DroneMissionAssignment assignment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = DroneMissionAssignmentMapper.toEntity.apply(assignment);
                var saved = repository.save(entity);

                log.info("✅ Saved drone assignment: drone {} to mission {}",
                        saved.getDroneId(), saved.getMissionId());

                return DroneMissionAssignmentMapper.toDomain.apply(saved);
            } catch (DataIntegrityViolationException e) {
                // Violación del UNIQUE constraint (mission_id, drone_id)
                log.error("❌ Drone {} already assigned to mission {}",
                        assignment.droneId(), assignment.missionId(), e);
                throw new DuplicateAssignmentException(
                        String.format("Drone %s is already assigned to mission %s",
                                assignment.droneId(), assignment.missionId()),
                        e
                );
            } catch (Exception e) {
                log.error("❌ Error saving drone assignment: drone {} to mission {}",
                        assignment.droneId(), assignment.missionId(), e);
                throw new DatabaseOperationException(
                        "Failed to save drone assignment", e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<DroneMissionAssignment>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(DroneMissionAssignmentMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding assignment by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<DroneMissionAssignment>> findByMissionId(String missionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByMissionId(UUID.fromString(missionId))
                        .stream()
                        .map(DroneMissionAssignmentMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding assignments for mission: {}", missionId, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<DroneMissionAssignment>> findByDroneId(String droneId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByDroneId(UUID.fromString(droneId))
                        .stream()
                        .map(DroneMissionAssignmentMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding assignments for drone: {}", droneId, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<DroneMissionAssignment>> findByMissionIdAndDroneId(
            String missionId,
            String droneId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByMissionIdAndDroneId(
                        UUID.fromString(missionId),
                        UUID.fromString(droneId)
                ).map(DroneMissionAssignmentMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding assignment for mission {} and drone {}",
                        missionId, droneId, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Boolean> existsByMissionIdAndDroneId(
            String missionId,
            String droneId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.existsByMissionIdAndDroneId(
                        UUID.fromString(missionId),
                        UUID.fromString(droneId)
                );
            } catch (Exception e) {
                log.error("❌ Error checking if assignment exists for mission {} and drone {}",
                        missionId, droneId, e);
                return false;
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Long> countByMissionId(String missionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.countByMissionId(UUID.fromString(missionId));
            } catch (Exception e) {
                log.error("❌ Error counting assignments for mission: {}", missionId, e);
                return 0L;
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<DroneMissionAssignment>> findByRouteId(String routeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByRouteId(UUID.fromString(routeId))
                        .stream()
                        .map(DroneMissionAssignmentMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding assignments for route: {}", routeId, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Void> deleteById(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                repository.deleteById(UUID.fromString(id));
                log.info("✅ Deleted assignment: {}", id);
            } catch (Exception e) {
                log.error("❌ Error deleting assignment: {}", id, e);
                throw new DatabaseOperationException(
                        "Failed to delete assignment: " + id, e);
            }
        });
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Void> deleteByMissionId(String missionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                repository.findByMissionId(UUID.fromString(missionId))
                        .forEach(repository::delete);
                log.info("✅ Deleted all assignments for mission: {}", missionId);
            } catch (Exception e) {
                log.error("❌ Error deleting assignments for mission: {}", missionId, e);
                throw new DatabaseOperationException(
                        "Failed to delete assignments for mission: " + missionId, e);
            }
        });
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DuplicateAssignmentException extends RuntimeException {
        public DuplicateAssignmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}