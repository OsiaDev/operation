package co.cetad.umas.operation.infrastructure.messaging.kafka.producer;

import co.cetad.umas.operation.domain.model.dto.MissionExecutionCommand;
import co.cetad.umas.operation.domain.ports.out.MissionExecutionPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Producer de Kafka para comandos de ejecución de misiones
 * Publica comandos al tópico de ejecución para que el servicio core los procese
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionExecutionKafkaProducer implements MissionExecutionPublisher {

    @Value("${kafka.topics.execute:umas.drone.execute}")
    private String executeTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publica un comando de ejecución de misión al tópico de Kafka
     *
     * @param command Comando de ejecución con waypoints de la ruta
     * @return CompletableFuture que se completa cuando el mensaje se envía exitosamente
     */
    @Override
    @Async
    public CompletableFuture<Void> publishExecutionCommand(MissionExecutionCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Serializar comando a JSON
                String messageJson = objectMapper.writeValueAsString(command);

                log.info("📤 Publishing execution command for mission: {} to topic: {}",
                        command.missionId(), executeTopic);

                log.debug("Execution command details: vehicleId={}, missionId={}, waypoints={}",
                        command.vehicleId(), command.missionId(), command.waypoints().size());

                // Enviar mensaje a Kafka usando vehicleId como key para partitioning
                CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                        executeTopic,
                        command.vehicleId(),  // Key: vehicleId para partitioning
                        messageJson
                );

                // Esperar confirmación de envío
                SendResult<String, String> result = future.join();

                log.info("✅ Execution command published successfully for mission: {} " +
                                "to partition: {} at offset: {}",
                        command.missionId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());

                return null;

            } catch (Exception e) {
                log.error("❌ Failed to publish execution command for mission: {}",
                        command.missionId(), e);
                throw new MessagePublishException(
                        "Failed to publish execution command for mission: " + command.missionId(),
                        e
                );
            }
        });
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