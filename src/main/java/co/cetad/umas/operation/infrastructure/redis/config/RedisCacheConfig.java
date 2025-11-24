package co.cetad.umas.operation.infrastructure.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.Locale;

/**
 * Configuración de Redis Cache SOLO para tabla de drones
 *
 * Cache Strategy:
 * - ✅ Drones: Cacheados por vehicleId (evita consultas repetidas en flujo de telemetría)
 * - ❌ Telemetría: NO se cachea (toda telemetría entrante debe almacenarse en BD)
 *
 * SOLUCIÓN AL PROBLEMA DE SERIALIZACIÓN:
 * - Cache almacena Drone directamente (no Optional<Drone>)
 * - ObjectMapper configurado con Jdk8Module para manejo de Optional cuando sea necesario
 * - PolymorphicTypeValidator para seguridad en deserialización
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisCacheConfig {

    /**
     * Configura ObjectMapper específico para Redis con:
     * - Jdk8Module: Soporte para Optional, OptionalInt, etc.
     * - JavaTimeModule: Soporte para LocalDateTime, etc.
     * - PolymorphicTypeValidator: Seguridad en deserialización
     * - Locale.US: Punto decimal para números
     *
     * @return ObjectMapper configurado para Redis
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Registrar módulos necesarios
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());

        // Configurar formato de fechas
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configurar locale US para punto decimal
        mapper.setLocale(Locale.US);

        // Configurar type validator para seguridad
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        return mapper;
    }

    /**
     * Configura RedisCacheManager con serialización JSON usando ObjectMapper configurado
     *
     * Cache "droneCache":
     * - TTL: 30 minutos (drones no cambian frecuentemente)
     * - Key: vehicleId (usado en findByVehicleId)
     * - Value: Drone serializado directamente (NO Optional<Drone>)
     *
     * @param connectionFactory Factory de conexiones Redis
     * @param redisObjectMapper ObjectMapper configurado para Redis
     * @return RedisCacheManager configurado solo para drones
     */
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper
    ) {
        // Crear serializer con el ObjectMapper configurado
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Configuración base de cache
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()      // No cachear valores null
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer)
                );

        // Configuración específica para cache de drones
        // IMPORTANTE: Almacena Drone directamente, NO Optional<Drone>
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