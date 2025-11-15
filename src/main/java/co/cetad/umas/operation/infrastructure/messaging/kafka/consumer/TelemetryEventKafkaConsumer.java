package co.cetad.umas.operation.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.operation.application.service.DroneTelemetryService;
import co.cetad.umas.operation.domain.model.vo.TelemetryEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Consumer Kafka simplificado - Solo almacena telemetría
 *
 * Funcionalidad:
 * - Recibe eventos de Kafka
 * - Parsea JSON
 * - Almacena en BD
 * - Retry con backoff (infraestructura)
 * - Dead Letter Queue
 */
@Slf4j
@Component
public class TelemetryEventKafkaConsumer {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Value("${kafka.consumer.max-retries:3}")
    private int maxRetries;

    @Value("${kafka.consumer.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    @Value("${kafka.consumer.processing-timeout-seconds:30}")
    private long processingTimeoutSeconds;

    private final DroneTelemetryService telemetryService;
    private final ObjectMapper objectMapper;
    private final Map<String, Integer> retryTracker = new HashMap<>();

    public TelemetryEventKafkaConsumer(DroneTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @KafkaListener(topics = "${kafka.topics.telemetry:umas.drone.telemetry}")
    public CompletableFuture<Void> handleTelemetryEvent(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment
    ) {
        log.debug("Received telemetry event [offset={}]", offset);

        return parseEvent(eventJson)
                .map(event -> processWithRetry(event, messageKey, acknowledgment))
                .orElseGet(() -> handleInvalidMessage(eventJson, acknowledgment));
    }

    /**
     * Procesa el evento con retry strategy
     */
    private CompletableFuture<Void> processWithRetry(
            TelemetryEvent event,
            String messageKey,
            Acknowledgment acknowledgment
    ) {
        String retryKey = generateRetryKey(messageKey, event.vehicleId());
        int attemptCount = retryTracker.getOrDefault(retryKey, 0);

        return telemetryService
                .process(event)
                .orTimeout(processingTimeoutSeconds, TimeUnit.SECONDS)
                .thenAccept(telemetry -> {
                    log.info("✅ Stored telemetry: {} for vehicle: {}",
                            telemetry.id(), event.vehicleId());
                    retryTracker.remove(retryKey);
                    acknowledgment.acknowledge();
                })
                .exceptionally(throwable -> {
                    return handleProcessingError(
                            event, retryKey, attemptCount, acknowledgment, throwable
                    );
                });
    }

    /**
     * Maneja errores con retry y DLQ
     */
    private Void handleProcessingError(
            TelemetryEvent event,
            String retryKey,
            int attemptCount,
            Acknowledgment acknowledgment,
            Throwable throwable
    ) {
        int newAttemptCount = attemptCount + 1;

        if (newAttemptCount < maxRetries) {
            long backoffTime = calculateBackoff(newAttemptCount);

            log.warn("⚠️ Error storing telemetry for vehicle: {} [attempt={}/{}]. " +
                            "Retrying in {}ms...",
                    event.vehicleId(), newAttemptCount, maxRetries, backoffTime, throwable);

            retryTracker.put(retryKey, newAttemptCount);
            return null; // No acknowledge para reintentar
        } else {
            log.error("❌ Max retries reached for vehicle: {} [attempts={}]. " +
                            "Sending to DLQ...",
                    event.vehicleId(), newAttemptCount, throwable);

            sendToDeadLetterQueue(event, throwable);
            retryTracker.remove(retryKey);
            acknowledgment.acknowledge();
            return null;
        }
    }

    /**
     * Maneja mensajes inválidos
     */
    private CompletableFuture<Void> handleInvalidMessage(
            String eventJson,
            Acknowledgment acknowledgment
    ) {
        log.error("❌ Invalid message format, sending to DLQ: {}", eventJson);
        sendInvalidMessageToDLQ(eventJson);
        acknowledgment.acknowledge();
        return CompletableFuture.completedFuture(null);
    }

    private long calculateBackoff(int attemptCount) {
        return retryBackoffMs * (long) Math.pow(2, attemptCount - 1);
    }

    private String generateRetryKey(String messageKey, String vehicleId) {
        return String.format("%s:%s", messageKey != null ? messageKey : "unknown", vehicleId);
    }

    /**
     * TODO: Implementar productor a topic DLQ
     */
    private void sendToDeadLetterQueue(TelemetryEvent event, Throwable error) {
        log.error("DLQ: Failed event for vehicle={}, error={}",
                event.vehicleId(), error.getMessage());
    }

    private void sendInvalidMessageToDLQ(String invalidMessage) {
        log.error("DLQ: Invalid message: {}", invalidMessage);
    }

    /**
     * Parsea el JSON a TelemetryEvent
     */
    private Optional<TelemetryEvent> parseEvent(String eventJson) {
        try {
            JsonNode root = objectMapper.readTree(eventJson);

            return Optional.of(new TelemetryEvent(
                    extractVehicleId(root),
                    extractLatitude(root),
                    extractLongitude(root),
                    extractAltitude(root),
                    extractSpeed(root),
                    extractHeading(root),
                    extractBatteryLevel(root),
                    extractSatelliteCount(root),
                    extractTimestamp(root),
                    extractAdditionalFields(root)
            ));

        } catch (Exception e) {
            log.error("Error parsing telemetry event: {}", eventJson, e);
            return Optional.empty();
        }
    }

    // ========== Métodos de extracción ==========

    private String extractVehicleId(JsonNode root) {
        return Optional.ofNullable(root.get("vehicleId"))
                .map(JsonNode::asText)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Missing vehicleId"));
    }

    private double extractLatitude(JsonNode root) {
        return Optional.ofNullable(root.get("latitude"))
                .map(JsonNode::asDouble)
                .orElseThrow(() -> new IllegalArgumentException("Missing latitude"));
    }

    private double extractLongitude(JsonNode root) {
        return Optional.ofNullable(root.get("longitude"))
                .map(JsonNode::asDouble)
                .orElseThrow(() -> new IllegalArgumentException("Missing longitude"));
    }

    private double extractAltitude(JsonNode root) {
        return Optional.ofNullable(root.get("altitude"))
                .map(JsonNode::asDouble)
                .orElse(0.0);
    }

    private Double extractSpeed(JsonNode root) {
        return extractOptionalDouble(root, "speed");
    }

    private Double extractHeading(JsonNode root) {
        return extractOptionalDouble(root, "heading");
    }

    private Double extractBatteryLevel(JsonNode root) {
        return extractOptionalDouble(root, "batteryLevel");
    }

    private Integer extractSatelliteCount(JsonNode root) {
        return Optional.ofNullable(root.get("satelliteCount"))
                .map(JsonNode::asInt)
                .filter(count -> count >= 0)
                .orElse(null);
    }

    private LocalDateTime extractTimestamp(JsonNode root) {
        return Optional.ofNullable(root.get("timestamp"))
                .map(JsonNode::asText)
                .flatMap(this::parseTimestamp)
                .orElseGet(LocalDateTime::now);
    }

    private Map<String, Object> extractAdditionalFields(JsonNode root) {
        return Optional.ofNullable(root.get("additionalFields"))
                .filter(JsonNode::isObject)
                .map(this::nodeToMap)
                .orElse(Map.of());
    }

    private Double extractOptionalDouble(JsonNode root, String fieldName) {
        return Optional.ofNullable(root.get(fieldName))
                .filter(node -> !node.isNull())
                .map(JsonNode::asDouble)
                .orElse(null);
    }

    private Optional<LocalDateTime> parseTimestamp(String timestamp) {
        try {
            return Optional.of(LocalDateTime.parse(timestamp, ISO_FORMATTER));
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse timestamp: {}", timestamp);
            return Optional.empty();
        }
    }

    private Map<String, Object> nodeToMap(JsonNode node) {
        Map<String, Object> result = new HashMap<>();

        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isNull()) {
                result.put(key, null);
            } else if (value.isBoolean()) {
                result.put(key, value.asBoolean());
            } else if (value.isInt()) {
                result.put(key, value.asInt());
            } else if (value.isLong()) {
                result.put(key, value.asLong());
            } else if (value.isDouble() || value.isFloat()) {
                result.put(key, value.asDouble());
            } else if (value.isTextual()) {
                result.put(key, value.asText());
            } else {
                result.put(key, value.toString());
            }
        });

        return result;
    }

}