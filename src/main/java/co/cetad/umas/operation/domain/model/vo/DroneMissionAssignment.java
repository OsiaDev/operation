package co.cetad.umas.operation.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa la ASIGNACIÓN de un dron a una misión
 *
 * REFACTORIZACIÓN: Nueva entidad para manejar la relación muchos-a-muchos
 * entre misiones y drones
 *
 * Características:
 * - Una misión puede tener múltiples drones asignados
 * - Un dron puede estar asignado a múltiples misiones (en diferentes tiempos)
 * - Cada asignación puede tener su propia ruta específica
 * - UNIQUE constraint: Un dron NO puede estar asignado dos veces a la misma misión
 *
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record DroneMissionAssignment(
        String id,
        String missionId,
        String droneId,
        String routeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isNew
) {

    /**
     * Constructor compacto con validaciones
     */
    public DroneMissionAssignment {
        Objects.requireNonNull(id, "Assignment ID cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(droneId, "Drone ID cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Assignment ID cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (droneId.isBlank()) {
            throw new IllegalArgumentException("Drone ID cannot be empty");
        }
    }

    /**
     * Factory method para crear una nueva asignación de dron a misión
     *
     * @param missionId ID de la misión
     * @param droneId ID del dron a asignar
     * @param routeId ID de la ruta para este dron (puede ser null)
     * @return Nueva instancia de DroneMissionAssignment
     */
    public static DroneMissionAssignment create(
            String missionId,
            String droneId,
            String routeId
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new DroneMissionAssignment(
                UUID.randomUUID().toString(),
                missionId,
                droneId,
                routeId,
                now,
                now,
                true
        );
    }

    /**
     * Verifica si la asignación tiene una ruta configurada
     */
    public boolean hasRoute() {
        return routeId != null && !routeId.isBlank();
    }

    /**
     * Crea una copia de la asignación con una nueva ruta
     */
    public DroneMissionAssignment withRoute(String newRouteId) {
        return new DroneMissionAssignment(
                id,
                missionId,
                droneId,
                newRouteId,
                createdAt,
                LocalDateTime.now(),
                isNew
        );
    }

    /**
     * Crea una copia de la asignación sin ruta
     */
    public DroneMissionAssignment withoutRoute() {
        return new DroneMissionAssignment(
                id,
                missionId,
                droneId,
                null,
                createdAt,
                LocalDateTime.now(),
                isNew
        );
    }

}