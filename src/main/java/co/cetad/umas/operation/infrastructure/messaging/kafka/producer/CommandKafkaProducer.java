package co.cetad.umas.operation.infrastructure.messaging.kafka.producer;

import co.cetad.umas.operation.domain.model.dto.ExecutionCommand;
import co.cetad.umas.operation.domain.ports.out.CommandExecutionPublisher;
import co.cetad.umas.operation.infrastructure.messaging.kafka.config.KafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer de Kafka para comandos de ejecución de drones
 * Publica comandos al tópico de ejecución para que el servicio core los procese
 *
 * IMPORTANTE: Basado en el patrón del TelemetryPublisher
 * - NO usa .join() para evitar bloqueos
 * - Convierte el CompletableFuture de Kafka directamente
 * - Usa .thenAccept() y .exceptionally() para manejo asíncrono
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandKafkaProducer implements CommandExecutionPublisher {

    private final KafkaTopicsProperties topics;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publica un comando de ejecución al tópico de Kafka
     *
     * Basado en el patrón del TelemetryPublisher pero adaptado para CompletableFuture
     * No usa .join() para evitar bloqueos
     *
     * @param command Comando de ejecución con código UGCS
     * @return CompletableFuture que se completa cuando el mensaje se envía exitosamente
     */
    @Override
    @Async
    public CompletableFuture<Void> publishExecutionCommand(ExecutionCommand command) {
        try {
            // Serializar comando a JSON
            String messageJson = objectMapper.writeValueAsString(command);

            log.info("📤 Publishing command '{}' for mission: {} (vehicleId: {}) to topic: {}",
                    command.commandCode(), command.missionId(), command.vehicleId(), topics.getExecute());

            log.debug("Full command details: {}", messageJson);

            // Enviar mensaje a Kafka usando vehicleId como key para partitioning
            // NO usar .join() - convertir directamente a CompletableFuture
            CompletableFuture<SendResult<String, String>> kafkaFuture = kafkaTemplate.send(
                    topics.getExecute(),
                    command.vehicleId(),  // Key: vehicleId para partitioning
                    messageJson
            ).toCompletableFuture();

            // Transformar el resultado a CompletableFuture<Void>
            return kafkaFuture
                    .thenAccept(result -> {
                        log.info("✅ Command '{}' published successfully for mission: {} " +
                                        "to partition: {} at offset: {}",
                                command.commandCode(),
                                command.missionId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    })
                    .exceptionally(e -> {
                        log.error("❌ Failed to publish command '{}' for mission: {}",
                                command.commandCode(), command.missionId(), e);
                        throw new MessagePublishException(
                                String.format("Failed to publish command '%s' for mission: %s",
                                        command.commandCode(), command.missionId()),
                                e
                        );
                    });

        } catch (Exception e) {
            // Error en serialización u otros problemas sincrónicos
            log.error("❌ Failed to serialize command '{}' for mission: {}",
                    command.commandCode(), command.missionId(), e);
            return CompletableFuture.failedFuture(
                    new MessagePublishException(
                            String.format("Failed to serialize command '%s' for mission: %s",
                                    command.commandCode(), command.missionId()),
                            e
                    )
            );
        }
    }

    /**
     * Excepción personalizada para errores de publicación de mensajes
     */
    public static class MessagePublishException extends RuntimeException {
        public MessagePublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}