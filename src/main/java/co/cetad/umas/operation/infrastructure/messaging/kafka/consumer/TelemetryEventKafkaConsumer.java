package co.cetad.umas.operation.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.operation.application.service.DroneTelemetryService;
import co.cetad.umas.operation.domain.model.vo.TelemetryEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Consumer de Kafka que procesa eventos de telemetría de drones
 * Topic: umas.drone.telemetry
 *
 * Actualizado para el nuevo formato con vehicleId y campos planos
 */
@Slf4j
@Component
public class TelemetryEventKafkaConsumer {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private final DroneTelemetryService telemetryService;
    private final ObjectMapper objectMapper;

    public TelemetryEventKafkaConsumer(DroneTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @KafkaListener(
            topics = "umas.drone.telemetry"
    )
    public CompletableFuture<Void> handleTelemetryEvent(
            @Payload String eventJson,
            Acknowledgment acknowledgment
    ) {
        log.info("Received telemetry event: {}", eventJson);

        return parseEvent(eventJson)
                .map(event -> processEventAsync(event, acknowledgment))
                .orElseGet(() -> {
                    log.warn("Failed to parse telemetry event, acknowledging anyway");
                    acknowledgment.acknowledge();
                    return CompletableFuture.completedFuture(null);
                });
    }

    private CompletableFuture<Void> processEventAsync(
            TelemetryEvent event,
            Acknowledgment acknowledgment
    ) {
        return telemetryService
                .process(event)
                .thenAccept(telemetry -> {
                    log.info("Processed telemetry: {} for vehicle: {}",
                            telemetry.id(), event.vehicleId());
                    acknowledgment.acknowledge();
                })
                .exceptionally(throwable -> {
                    log.error("Error processing telemetry event for vehicle: {}",
                            event.vehicleId(), throwable);
                    // No acknowledge para reintentar
                    return null;
                });
    }

    /**
     * Parsea el JSON del evento a un objeto TelemetryEvent
     * Implementación funcional con Optional y validaciones
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

    // ========== Métodos de extracción con validación funcional ==========

    private String extractVehicleId(JsonNode root) {
        return Optional.ofNullable(root.get("vehicleId"))
                .map(JsonNode::asText)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Missing or empty vehicleId"));
    }

    private double extractLatitude(JsonNode root) {
        return Optional.ofNullable(root.get("latitude"))
                .map(JsonNode::asDouble)
                .filter(this::isValidLatitude)
                .orElseThrow(() -> new IllegalArgumentException("Missing or invalid latitude"));
    }

    private double extractLongitude(JsonNode root) {
        return Optional.ofNullable(root.get("longitude"))
                .map(JsonNode::asDouble)
                .filter(this::isValidLongitude)
                .orElseThrow(() -> new IllegalArgumentException("Missing or invalid longitude"));
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
                .orElseGet(() -> {
                    log.debug("No timestamp provided, using current time");
                    return LocalDateTime.now();
                });
    }

    private Map<String, Object> extractAdditionalFields(JsonNode root) {
        return Optional.ofNullable(root.get("additionalFields"))
                .filter(JsonNode::isObject)
                .map(this::nodeToMap)
                .orElse(Map.of());
    }

    // ========== Métodos helper ==========

    private Double extractOptionalDouble(JsonNode root, String fieldName) {
        return Optional.ofNullable(root.get(fieldName))
                .filter(node -> !node.isNull())
                .map(JsonNode::asDouble)
                .orElse(null);
    }

    private boolean isValidLatitude(double latitude) {
        return latitude >= -90.0 && latitude <= 90.0;
    }

    private boolean isValidLongitude(double longitude) {
        return longitude >= -180.0 && longitude <= 180.0;
    }

    private Optional<LocalDateTime> parseTimestamp(String timestamp) {
        try {
            return Optional.of(LocalDateTime.parse(timestamp, ISO_FORMATTER));
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse timestamp: {}", timestamp);
            return Optional.empty();
        }
    }

    /**
     * Convierte un JsonNode de tipo objeto a un Map<String, Object>
     * Útil para additionalFields
     */
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
                // Para objetos o arrays complejos, guardar como string
                result.put(key, value.toString());
            }
        });

        return result;
    }

}