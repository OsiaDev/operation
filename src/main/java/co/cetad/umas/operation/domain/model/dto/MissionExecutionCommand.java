package co.cetad.umas.operation.domain.model.dto;

import co.cetad.umas.operation.domain.model.vo.Waypoint;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * DTO para enviar comando de ejecución de misión a través de Kafka
 * Contiene la información de TODOS los drones asignados a la misión con sus rutas
 *
 * REFACTORIZACIÓN: Ahora envía lista de drones con sus respectivos waypoints y routeId
 * en lugar de un comando por dron
 */
public record MissionExecutionCommand(
        @JsonProperty("missionId") String missionId,
        @JsonProperty("drones") List<DroneExecution> drones,
        @JsonProperty("priority") Integer priority
) {

    public MissionExecutionCommand {
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(drones, "Drones cannot be null");

        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (drones.isEmpty()) {
            throw new IllegalArgumentException("Drones list cannot be empty");
        }
        if (priority == null || priority < 0) {
            priority = 0;
        }
    }

    /**
     * Factory method para crear comando de ejecución de misión
     *
     * @param missionId ID de la misión
     * @param drones Lista de drones con sus waypoints
     * @return Comando de ejecución configurado
     */
    public static MissionExecutionCommand create(
            String missionId,
            List<DroneExecution> drones
    ) {
        return new MissionExecutionCommand(
                missionId,
                drones,
                1  // Prioridad normal
        );
    }

    /**
     * DTO que representa la ejecución de un dron específico con su ruta
     *
     * ACTUALIZACIÓN: Ahora incluye routeId para identificar la ruta asignada
     */
    public record DroneExecution(
            @JsonProperty("vehicleId") String vehicleId,
            @JsonProperty("routeId") String routeId,
            @JsonProperty("waypoints") List<Waypoint> waypoints
    ) {
        public DroneExecution {
            Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
            Objects.requireNonNull(waypoints, "Waypoints cannot be null");

            if (vehicleId.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID cannot be empty");
            }
            // routeId puede ser null (drones sin ruta asignada)
            // Permitir lista vacía de waypoints (drones sin ruta asignada)
        }

        /**
         * Factory method para crear ejecución de dron con waypoints y routeId
         */
        public static DroneExecution create(String vehicleId, String routeId, List<Waypoint> waypoints) {
            return new DroneExecution(vehicleId, routeId, waypoints);
        }

        /**
         * Factory method para crear ejecución de dron sin waypoints ni ruta
         */
        public static DroneExecution createWithoutRoute(String vehicleId) {
            return new DroneExecution(vehicleId, null, List.of());
        }

        /**
         * Verifica si el dron tiene waypoints asignados
         */
        public boolean hasWaypoints() {
            return !waypoints.isEmpty();
        }

        /**
         * Verifica si el dron tiene ruta asignada
         */
        public boolean hasRoute() {
            return routeId != null && !routeId.isBlank();
        }
    }

}