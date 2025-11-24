package co.cetad.umas.operation.domain.model.dto;

import co.cetad.umas.operation.domain.model.vo.Waypoint;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * DTO para enviar comando de ejecución de misión a través de Kafka
 * Contiene la información necesaria para que el dron ejecute la ruta
 */
public record MissionExecutionCommand(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("missionId") String missionId,
        @JsonProperty("commandCode") String commandCode,
        @JsonProperty("waypoints") List<Waypoint> waypoints,
        @JsonProperty("priority") Integer priority
) {

    public MissionExecutionCommand {
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(commandCode, "Command code cannot be null");
        Objects.requireNonNull(waypoints, "Waypoints cannot be null");

        if (vehicleId.isBlank()) {
            throw new IllegalArgumentException("Vehicle ID cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (commandCode.isBlank()) {
            throw new IllegalArgumentException("Command code cannot be empty");
        }
        if (waypoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoints list cannot be empty");
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
     * @param waypoints Lista de waypoints de la ruta
     * @return Comando de ejecución configurado
     */
    public static MissionExecutionCommand create(
            String vehicleId,
            String missionId,
            List<Waypoint> waypoints
    ) {
        return new MissionExecutionCommand(
                vehicleId,
                missionId,
                "EXECUTE_ROUTE",  // Código de comando para ejecutar ruta
                waypoints,
                1  // Prioridad normal
        );
    }

}