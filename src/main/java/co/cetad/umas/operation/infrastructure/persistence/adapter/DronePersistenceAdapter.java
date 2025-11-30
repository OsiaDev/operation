package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.DroneMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcDroneRepository;
import co.cetad.umas.operation.infrastructure.redis.config.RedisCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
 * REFACTORIZADO: Usa RedisTemplate tipado en lugar de CacheManager
 * para evitar problemas de serialización con LinkedHashMap
 *
 * Cache configurado SOLO para drones:
 * - findByVehicleId: Consulta RedisTemplate antes de buscar en BD
 * - save: Actualiza RedisTemplate después de guardar
 *
 * IMPORTANTE: RedisTemplate está tipado específicamente como <String, Drone>
 * lo que garantiza deserialización correcta sin LinkedHashMap
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DronePersistenceAdapter implements DroneRepository {

    private final R2dbcDroneRepository repository;
    private final RedisTemplate<String, Drone> droneRedisTemplate;

    /**
     * Guarda un dron y actualiza el cache usando RedisTemplate
     */
    @Override
    @Async
    public CompletableFuture<Drone> save(Drone drone) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Guardar en BD primero
                var entity = DroneMapper.toEntity.apply(drone);
                var saved = repository.save(entity);
                var savedDrone = DroneMapper.toDomain.apply(saved);

                log.info("✅ Saved drone: {} with vehicleId: {}",
                        saved.getId(), saved.getVehicleId());

                // Actualizar cache DESPUÉS de guardar exitosamente
                try {
                    String cacheKey = buildCacheKey(savedDrone.vehicleId());
                    droneRedisTemplate.opsForValue().set(
                            cacheKey,
                            savedDrone,
                            RedisCacheConfig.getDroneCacheTtl()
                    );
                    log.debug("💾 Cache updated for vehicleId: {}", savedDrone.vehicleId());
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
     * Busca un dron por vehicleId con cache usando RedisTemplate
     * Este es el método MÁS USADO en el flujo de telemetría
     *
     * REFACTORIZADO para usar RedisTemplate tipado:
     * - Consulta RedisTemplate directamente antes de ir a BD
     * - RedisTemplate<String, Drone> garantiza deserialización correcta
     * - Maneja errores de cache sin afectar la funcionalidad
     * - Actualiza cache solo si encuentra el dato en BD
     */
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<Drone>> findByVehicleId(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String cacheKey = buildCacheKey(vehicleId);

                // Intentar obtener del cache primero
                try {
                    Drone cachedDrone = droneRedisTemplate.opsForValue().get(cacheKey);
                    if (cachedDrone != null) {
                        log.debug("🎯 Cache HIT for vehicleId: {}", vehicleId);
                        return Optional.of(cachedDrone);
                    }
                } catch (Exception cacheError) {
                    // Si el cache falla, continuar con búsqueda en BD
                    log.warn("⚠️ Cache read failed for vehicleId: {}, falling back to DB",
                            vehicleId, cacheError);
                }

                // Cache MISS o error - buscar en BD
                log.debug("🔍 Cache MISS for vehicleId: {}. Searching in DB...", vehicleId);
                Optional<Drone> droneOpt = repository.findByVehicleId(vehicleId)
                        .map(DroneMapper.toDomain);

                // Si encontró en BD, intentar cachear
                if (droneOpt.isPresent()) {
                    try {
                        droneRedisTemplate.opsForValue().set(
                                cacheKey,
                                droneOpt.get(),
                                RedisCacheConfig.getDroneCacheTtl()
                        );
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
                // Primero verificar en cache usando RedisTemplate
                try {
                    String cacheKey = buildCacheKey(vehicleId);
                    Drone cachedDrone = droneRedisTemplate.opsForValue().get(cacheKey);
                    if (cachedDrone != null) {
                        log.debug("🎯 Cache HIT for exists check, vehicleId: {}", vehicleId);
                        return true;
                    }
                } catch (Exception cacheError) {
                    log.debug("⚠️ Cache read failed for exists check, falling back to DB");
                }

                // Cache MISS - verificar en BD
                return repository.existsByVehicleId(vehicleId);
            } catch (Exception e) {
                log.error("❌ Error checking if drone exists by vehicleId: {}", vehicleId, e);
                return false;
            }
        });
    }

    /**
     * Construye la key de cache con prefijo
     * Formato: umas:drone:vehicleId
     */
    private String buildCacheKey(String vehicleId) {
        return "umas:drone:" + vehicleId;
    }

    public static class DatabaseOperationException extends RuntimeException {
        public DatabaseOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}