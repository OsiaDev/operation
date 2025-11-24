package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.ports.out.DroneTelemetryRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneTelemetryMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneTelemetryRepository;
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
 * Adaptador de persistencia para telemetría usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * IMPORTANTE: NO hay cache para telemetría
 * - Toda telemetría entrante DEBE almacenarse en BD
 * - Cache solo se usa para tabla de drones
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DroneTelemetryPersistenceAdapter implements DroneTelemetryRepository {

    private final R2dbcDroneTelemetryRepository repository;

    /**
     * Guarda telemetría en BD - SIN CACHE
     * Toda telemetría debe persistirse siempre
     */
    @Override
    @Async
    @Transactional
    public CompletableFuture<DroneTelemetry> save(DroneTelemetry telemetry) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = DroneTelemetryMapper.toEntity.apply(telemetry);
                var saved = repository.save(entity);

                log.debug("✅ Saved telemetry: {} for vehicle: {} at {}",
                        saved.getId(), saved.getVehicleId(), saved.getTimestamp());

                return DroneTelemetryMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving telemetry for vehicle: {}",
                        telemetry.vehicleId(), e);
                throw new DatabaseOperationException(
                        "Failed to save telemetry for vehicle: " + telemetry.vehicleId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<DroneTelemetry>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(DroneTelemetryMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding telemetry by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<DroneTelemetry>> findLatestByVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findLatestByVehicleId(vehicleId)
                        .map(DroneTelemetryMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding latest telemetry for vehicle: {}", vehicleId, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<DroneTelemetry>> findByVehicleIdAndDateRange(
            String vehicleId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findByVehicleIdAndDateRange(vehicleId, startDate, endDate)
                        .stream()
                        .map(DroneTelemetryMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding telemetry for vehicle: {} in date range",
                        vehicleId, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<DroneTelemetry>> findRecentByVehicleId(
            String vehicleId,
            int limit
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findRecentByVehicleId(vehicleId, limit)
                        .stream()
                        .map(DroneTelemetryMapper.toDomain)
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error finding recent telemetry for vehicle: {}", vehicleId, e);
                return List.of();
            }
        });
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<Long> deleteOlderThan(LocalDateTime date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long count = repository.deleteOlderThan(date);
                log.info("Deleted {} telemetry records older than {}", count, date);
                return count;
            } catch (Exception e) {
                log.error("❌ Error deleting old telemetry records", e);
                return 0L;
            }
        });
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}