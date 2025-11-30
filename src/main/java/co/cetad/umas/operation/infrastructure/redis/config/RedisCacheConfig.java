package co.cetad.umas.operation.infrastructure.redis.config;

import co.cetad.umas.operation.domain.model.vo.Drone;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Locale;

/**
 * Configuración de Redis SOLO para tabla de drones
 *
 * Cache Strategy:
 * - ✅ Drones: Cacheados por vehicleId (evita consultas repetidas en flujo de telemetría)
 * - ❌ Telemetría: NO se cachea (toda telemetría entrante debe almacenarse en BD)
 *
 * SOLUCIÓN DEFINITIVA AL PROBLEMA DE SERIALIZACIÓN:
 * - Usa RedisTemplate con Jackson2JsonRedisSerializer tipado específicamente para Drone
 * - NO usa GenericJackson2JsonRedisSerializer que causa problemas con LinkedHashMap
 * - ObjectMapper configurado para manejar records de Java 21
 * - TTL de 30 minutos para drones
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisCacheConfig {

    private static final Duration DRONE_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Configura ObjectMapper específico para Redis con:
     * - JavaTimeModule: Soporte para LocalDateTime, etc.
     * - Locale.US: Punto decimal para números
     * - Configuración para records de Java 21
     *
     * @return ObjectMapper configurado para Redis
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Registrar módulo de tiempo
        mapper.registerModule(new JavaTimeModule());

        // Configurar formato de fechas
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Configurar locale US para punto decimal
        mapper.setLocale(Locale.US);

        return mapper;
    }

    /**
     * Configura RedisTemplate específicamente tipado para Drone
     * Esto evita problemas de deserialización con LinkedHashMap
     *
     * USO:
     * - Key: vehicleId (String)
     * - Value: Drone (record)
     * - TTL: 30 minutos (configurado en el adaptador)
     *
     * @param connectionFactory Factory de conexiones Redis
     * @param redisObjectMapper ObjectMapper configurado para Redis
     * @return RedisTemplate tipado para String keys y Drone values
     */
    @Bean
    public RedisTemplate<String, Drone> droneRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper
    ) {
        RedisTemplate<String, Drone> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer para keys (String)
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // Serializer para values (Drone) - TIPADO ESPECÍFICAMENTE
        // Esto garantiza que Redis deserialice correctamente como Drone
        Jackson2JsonRedisSerializer<Drone> valueSerializer =
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Drone.class);

        // Configurar serializers
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }

    /**
     * Getter para TTL de cache de drones
     * Usado por el adaptador para configurar expiración
     */
    public static Duration getDroneCacheTtl() {
        return DRONE_CACHE_TTL;
    }

}