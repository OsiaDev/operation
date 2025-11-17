package co.cetad.umas.operation.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una operación de dron
 * Una operación es una orden de vuelo asignada a un dron específico
 */
public record DroneOperation(
        String id,
        String droneId,
        String routeId,
        LocalDateTime startDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * Constructor compacto con validaciones
     */
    public DroneOperation {
        Objects.requireNonNull(id, "Operation ID cannot be null");
        Objects.requireNonNull(droneId, "Drone ID cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Operation ID cannot be empty");
        }
        if (droneId.isBlank()) {
            throw new IllegalArgumentException("Drone ID cannot be empty");
        }
    }

    /**
     * Factory method para crear una nueva operación
     * Genera automáticamente el ID y las fechas de auditoría
     *
     * @param droneId ID del dron asignado
     * @param routeId ID de la ruta (puede ser null)
     * @param startDate Fecha de inicio de la operación
     * @return Nueva instancia de DroneOperation
     */
    public static DroneOperation create(
            String droneId,
            String routeId,
            LocalDateTime startDate
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new DroneOperation(
                UUID.randomUUID().toString(),
                droneId,
                routeId,
                startDate,
                now,
                now
        );
    }

    /**
     * Verifica si la operación tiene una ruta asignada
     */
    public boolean hasRoute() {
        return routeId != null && !routeId.isBlank();
    }

    /**
     * Verifica si la operación está programada para el futuro
     */
    public boolean isScheduledForFuture() {
        return startDate.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si la operación ya debería haber comenzado
     */
    public boolean shouldHaveStarted() {
        return !startDate.isAfter(LocalDateTime.now());
    }

    /**
     * Crea una copia de la operación con una nueva ruta asignada
     */
    public DroneOperation withRoute(String newRouteId) {
        return new DroneOperation(
                id,
                droneId,
                newRouteId,
                startDate,
                createdAt,
                LocalDateTime.now() // Actualiza updatedAt
        );
    }

    /**
     * Crea una copia de la operación con una nueva fecha de inicio
     */
    public DroneOperation withStartDate(LocalDateTime newStartDate) {
        Objects.requireNonNull(newStartDate, "New start date cannot be null");
        return new DroneOperation(
                id,
                droneId,
                routeId,
                newStartDate,
                createdAt,
                LocalDateTime.now() // Actualiza updatedAt
        );
    }

}