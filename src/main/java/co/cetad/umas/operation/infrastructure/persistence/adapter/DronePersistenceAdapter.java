package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
 * REFACTORIZADO: Uso directo de CacheManager en lugar de anotaciones
 * para evitar problemas de race conditions y serialización
 *
 * Cache configurado SOLO para drones:
 * - findByVehicleId: Consulta cache manualmente antes de buscar en BD
 * - save: Actualiza cache manualmente después de guardar
 *
 * IMPORTANTE: El cache almacena Drone directamente (no Optional<Drone>)
 * para evitar problemas de serialización con Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DronePersistenceAdapter implements DroneRepository {

    private final R2dbcDroneRepository repository;
    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "droneCache";

    /**
     * Guarda un dron y actualiza el cache manualmente
     * Usa CacheManager directamente en lugar de @CachePut para mejor control
     */
    @Override
    @Async
    public CompletableFuture<Drone> save(Drone drone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Guardar en BD primero (sin transacción para evitar problemas con CompletableFuture)
                var entity = DroneMapper.toEntity.apply(drone);
                var saved = repository.save(entity);
                var savedDrone = DroneMapper.toDomain.apply(saved);

                log.info("✅ Saved drone: {} with vehicleId: {}",
                        saved.getId(), saved.getVehicleId());

                // Actualizar cache DESPUÉS de guardar exitosamente
                // Con try-catch para que errores de cache no afecten la persistencia
                try {
                    Cache cache = cacheManager.getCache(CACHE_NAME);
                    if (cache != null) {
                        cache.put(savedDrone.vehicleId(), savedDrone);
                        log.debug("💾 Cache updated for vehicleId: {}", savedDrone.vehicleId());
                    }
                } catch (Exception cacheError) {
                    // NO fallar si el cache falla - el dato ya está en BD
                    log.warn("⚠️ Failed to update cache for vehicleId: {} (drone saved successfully)",
                            savedDrone.vehicleId(), cacheError);
                }

                return savedDrone;
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
     * Busca un dron por vehicleId con cache manual
     * Este es el método MÁS USADO en el flujo de telemetría
     *
     * REFACTORIZADO para usar CacheManager directamente:
     * - Consulta cache manualmente antes de ir a BD
     * - Maneja errores de cache sin afectar la funcionalidad
     * - Actualiza cache solo si encuentra el dato en BD
     * - Cache almacena Drone directamente (no Optional<Drone>)
     */
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Drone>> findByVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Intentar obtener del cache primero
                Cache cache = cacheManager.getCache(CACHE_NAME);
                if (cache != null) {
                    try {
                        Cache.ValueWrapper wrapper = cache.get(vehicleId);
                        if (wrapper != null && wrapper.get() != null) {
                            Drone cachedDrone = (Drone) wrapper.get();
                            log.debug("🎯 Cache HIT for vehicleId: {}", vehicleId);
                            return Optional.of(cachedDrone);
                        }
                    } catch (Exception cacheError) {
                        // Si el cache falla, continuar con búsqueda en BD
                        log.warn("⚠️ Cache read failed for vehicleId: {}, falling back to DB",
                                vehicleId, cacheError);
                    }
                }

                // Cache MISS o error - buscar en BD
                log.debug("🔍 Cache MISS for vehicleId: {}. Searching in DB...", vehicleId);
                Optional<Drone> droneOpt = repository.findByVehicleId(vehicleId)
                        .map(DroneMapper.toDomain);

                // Si encontró en BD, intentar cachear (con try-catch para no fallar si cache falla)
                if (droneOpt.isPresent() && cache != null) {
                    try {
                        cache.put(vehicleId, droneOpt.get());
                        log.debug("💾 Cached drone for vehicleId: {}", vehicleId);
                    } catch (Exception cacheError) {
                        // NO fallar si el cache falla - el dato está disponible
                        log.warn("⚠️ Failed to cache drone for vehicleId: {} (data available from DB)",
                                vehicleId, cacheError);
                    }
                }

                return droneOpt;
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
                // Primero verificar en cache
                Cache cache = cacheManager.getCache(CACHE_NAME);
                if (cache != null) {
                    try {
                        Cache.ValueWrapper wrapper = cache.get(vehicleId);
                        if (wrapper != null && wrapper.get() != null) {
                            log.debug("🎯 Cache HIT for exists check, vehicleId: {}", vehicleId);
                            return true;
                        }
                    } catch (Exception cacheError) {
                        log.debug("⚠️ Cache read failed for exists check, falling back to DB");
                    }
                }

                // Cache MISS - verificar en BD
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