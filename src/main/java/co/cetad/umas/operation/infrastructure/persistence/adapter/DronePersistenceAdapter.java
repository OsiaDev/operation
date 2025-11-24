package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para drones usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DronePersistenceAdapter implements DroneRepository {

    private final R2dbcDroneRepository repository;

    @Override
    @Async
    @Transactional
    public CompletableFuture<Drone> save(Drone drone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = DroneMapper.toEntity.apply(drone);
                var saved = repository.save(entity);

                log.info("✅ Saved drone: {} with vehicleId: {}",
                        saved.getId(), saved.getVehicleId());

                return DroneMapper.toDomain.apply(saved);
            } catch (Exception e) {
                log.error("❌ Error saving drone with vehicleId: {}",
                        drone.vehicleId(), e);
                throw new DatabaseOperationException(
                        "Failed to save drone with vehicleId: " + drone.vehicleId(), e);
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Drone>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.findById(UUID.fromString(id))
                        .map(DroneMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding drone by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Drone>> findByVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Searching drone by vehicleId: {}", vehicleId);
                return repository.findByVehicleId(vehicleId)
                        .map(DroneMapper.toDomain);
            } catch (Exception e) {
                log.error("❌ Error finding drone by vehicleId: {}", vehicleId, e);
                return Optional.empty();
            }
        });
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Boolean> existsByVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return repository.existsByVehicleId(vehicleId);
            } catch (Exception e) {
                log.error("❌ Error checking if drone exists by vehicleId: {}", vehicleId, e);
                return false;
            }
        });
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}