package co.cetad.umas.operation.infrastructure.messaging.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración mejorada de Kafka Consumer
 *
 * Mejoras:
 * - Configuración más granular
 * - Error handler con backoff exponencial
 * - Timeouts configurables
 * - Concurrencia ajustable
 * - Configuración de recursos optimizada
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.consumer.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:umas-operation-service}")
    private String groupId;

    @Value("${spring.kafka.consumer.concurrency:3}")
    private int concurrency;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private int maxPollRecords;

    @Value("${spring.kafka.consumer.max-poll-interval-ms:300000}")
    private int maxPollIntervalMs;

    @Value("${spring.kafka.consumer.session-timeout-ms:30000}")
    private int sessionTimeoutMs;

    @Value("${spring.kafka.consumer.heartbeat-interval-ms:10000}")
    private int heartbeatIntervalMs;

    @Value("${spring.kafka.consumer.fetch-min-bytes:1}")
    private int fetchMinBytes;

    @Value("${spring.kafka.consumer.fetch-max-wait-ms:500}")
    private int fetchMaxWaitMs;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Configuración básica
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Offset management
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual acknowledgment

        // Configuración de polling
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);

        // Configuración de sesión y heartbeat
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, sessionTimeoutMs + 10000);

        // Configuración de reconexión automática
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 500);
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);

        // Configuración de metadata
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, 5000);

        // Configuración de aislamiento (read_committed para transacciones)
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // Configuración de cliente
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, groupId + "-client");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // Configuración de acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Configuración de concurrencia
        factory.setConcurrency(concurrency);

        // Configuración de error handler con backoff exponencial
        factory.setCommonErrorHandler(errorHandler());

        // Configuración de filtros (opcional)
        // factory.setRecordFilterStrategy(record -> {
        //     // Filtrar mensajes si es necesario
        //     return false;
        // });

        // Configuración de batch processing (opcional)
        factory.setBatchListener(false);

        return factory;
    }

    /**
     * Error handler con backoff exponencial
     *
     * Configuración:
     * - Initial interval: 1000ms
     * - Multiplier: 2.0
     * - Max interval: 10000ms
     * - Max attempts: 3
     */
    @Bean
    public CommonErrorHandler errorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10000L);
        backOff.setMaxElapsedTime(30000L); // 30 segundos máximo

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);

        // Configurar excepciones que no deben reintentar
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                com.fasterxml.jackson.core.JsonParseException.class
        );

        return errorHandler;
    }

    /**
     * Factory para crear listeners específicos con configuración custom
     * Útil si necesitas múltiples listeners con diferentes configuraciones
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> highThroughputKafkaListenerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(concurrency * 2); // Doble concurrencia
        factory.setCommonErrorHandler(errorHandler());
        factory.setBatchListener(true); // Batch processing para high throughput

        return factory;
    }

}