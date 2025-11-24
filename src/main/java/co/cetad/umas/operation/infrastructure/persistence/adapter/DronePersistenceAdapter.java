package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para drones usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Cache configurado SOLO para drones:
 * - findByVehicleId: Cacheable (evita consultas repetidas)
 * - save: CachePut (actualiza cache después de guardar)
 *
 * IMPORTANTE: El cache almacena Drone directamente (no Optional<Drone>)
 * para evitar problemas de serialización con Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DronePersistenceAdapter implements DroneRepository {

    private final R2dbcDroneRepository repository;

    /**
     * Guarda un dron y actualiza el cache
     * @CachePut asegura que el cache se actualice con el nuevo valor
     *
     * Cache almacena Drone directamente (no Optional)
     */
    @Override
    @Async
    @Transactional
    @CachePut(value = "droneCache", key = "#drone.vehicleId",
            unless = "#drone == null")
    public CompletableFuture<Drone> save(Drone drone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var entity = DroneMapper.toEntity.apply(drone);
                var saved = repository.save(entity);

                log.info("✅ Saved drone: {} with vehicleId: {} (cache updated)",
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

    /**
     * Busca un dron por vehicleId con cache
     * Este es el método MÁS USADO en el flujo de telemetría
     *
     * SOLUCIÓN AL PROBLEMA DE SERIALIZACIÓN:
     * - El cache almacena Drone directamente (no Optional<Drone>)
     * - El método devuelve Optional<Drone> para mantener la interfaz
     * - Si encuentra en cache, lo envuelve en Optional
     * - Si no encuentra en cache, busca en BD y cachea el resultado
     *
     * @Cacheable evita consultas repetidas a BD para el mismo vehicleId
     */
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Drone>> findByVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Buscar en cache primero (usa método auxiliar)
                Drone cachedDrone = findByVehicleIdFromCache(vehicleId);

                if (cachedDrone != null) {
                    log.debug("🎯 Cache HIT for vehicleId: {}", vehicleId);
                    return Optional.of(cachedDrone);
                }

                // Cache MISS - buscar en BD
                log.debug("🔍 Cache MISS for vehicleId: {}. Searching in DB...", vehicleId);
                Optional<Drone> droneOpt = repository.findByVehicleId(vehicleId)
                        .map(DroneMapper.toDomain);

                // Si encontró en BD, cachear el resultado
                droneOpt.ifPresent(drone -> cacheVehicleId(vehicleId, drone));

                return droneOpt;
            } catch (Exception e) {
                log.error("❌ Error finding drone by vehicleId: {}", vehicleId, e);
                return Optional.empty();
            }
        });
    }

    /**
     * Método auxiliar que busca en cache y retorna Drone directamente
     * (no Optional) para evitar problemas de serialización
     *
     * @Cacheable almacena Drone directamente, no Optional<Drone>
     */
    @Cacheable(value = "droneCache", key = "#vehicleId", unless = "#result == null")
    public Drone findByVehicleIdFromCache(String vehicleId) {
        // Este método nunca se ejecuta si hay cache
        // Solo se usa para que @Cacheable funcione correctamente
        return null;
    }

    /**
     * Método auxiliar para cachear un dron por vehicleId
     * @CachePut almacena Drone directamente en cache
     */
    @CachePut(value = "droneCache", key = "#vehicleId", unless = "#drone == null")
    public Drone cacheVehicleId(String vehicleId, Drone drone) {
        log.debug("💾 Caching drone for vehicleId: {}", vehicleId);
        return drone;
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