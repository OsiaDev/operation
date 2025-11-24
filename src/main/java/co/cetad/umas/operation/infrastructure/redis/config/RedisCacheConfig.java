package co.cetad.umas.operation.infrastructure.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuración de Redis Cache SOLO para tabla de drones
 *
 * Cache Strategy:
 * - ✅ Drones: Cacheados por vehicleId (evita consultas repetidas en flujo de telemetría)
 * - ❌ Telemetría: NO se cachea (toda telemetría entrante debe almacenarse en BD)
 *
 * Esta configuración asegura que Redis use el ObjectMapper configurado
 * que incluye Jdk8Module para serialización de Optional<Drone>
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisCacheConfig {

    /**
     * Configura RedisCacheManager con serialización JSON usando ObjectMapper configurado
     *
     * Cache "droneCache":
     * - TTL: 30 minutos (drones no cambian frecuentemente)
     * - Key: vehicleId (usado en findByVehicleId)
     * - Value: Optional<Drone> serializado con Jdk8Module
     *
     * @param connectionFactory Factory de conexiones Redis
     * @param objectMapper ObjectMapper con Jdk8Module registrado
     * @return RedisCacheManager configurado solo para drones
     */
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        // Crear serializer con el ObjectMapper que tiene Jdk8Module
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Configuración base de cache
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()      // No cachear valores null/empty
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                );

        // Configuración específica para cache de drones
        RedisCacheConfiguration droneConfig = baseConfig
                .entryTtl(Duration.ofMinutes(30))  // TTL de 30 minutos
                .prefixCacheNameWith("umas:drone:");  // Prefijo para keys

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig.entryTtl(Duration.ofHours(1)))
                // Cache SOLO para drones
                .withCacheConfiguration("droneCache", droneConfig)
                // NO configuramos cache para telemetría - debe guardarse siempre
                .build();
    }

}