package co.cetad.umas.operation.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * DTO para enviar comando de ejecución de misión a través de Kafka
 * Contiene la información necesaria para que el dron ejecute la ruta
 */
public record ExecutionCommand(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("missionId") String missionId,
        @JsonProperty("commandCode") String commandCode,
        @JsonProperty("priority") Integer priority
) {

    public ExecutionCommand {
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(commandCode, "CommandCode cannot be null");

        if (vehicleId.isBlank()) {
            throw new IllegalArgumentException("Vehicle ID cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (commandCode.isBlank()) {
            throw new IllegalArgumentException("CommandCode cannot be empty");
        }
        if (priority == null || priority < 0) {
            priority = 0;
        }
    }

    /**
     * Factory method para crear comando de ejecución de misión
     *
     * @param vehicleId ID del vehículo/dron
     * @param missionId ID de la misión
     * @param commandCode Code de ugcs server
     * @return Comando de ejecución configurado
     */
    public static ExecutionCommand create(
            String vehicleId,
            String missionId,
            String commandCode
    ) {
        return new ExecutionCommand(
                vehicleId,
                missionId,
                commandCode,
                1  // Prioridad normal
        );
    }

}