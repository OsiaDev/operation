package co.cetad.umas.operation.infrastructure.messaging.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {

    private String execute = "umas.drone.execute";

}