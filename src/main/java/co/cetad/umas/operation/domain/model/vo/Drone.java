package co.cetad.umas.operation.domain.model.vo;

import co.cetad.umas.operation.domain.model.entity.DroneStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa un dron
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record Drone(
        String id,
        String name,
        String vehicleId,
        String model,
        String description,
        String serialNumber,
        DroneStatus status,
        BigDecimal flightHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * Constructor compacto con validaciones
     */
    public Drone {
        Objects.requireNonNull(id, "Drone ID cannot be null");
        Objects.requireNonNull(name, "Drone name cannot be null");
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        Objects.requireNonNull(flightHours, "Flight hours cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Drone ID cannot be empty");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Drone name cannot be empty");
        }
        if (vehicleId.isBlank()) {
            throw new IllegalArgumentException("Vehicle ID cannot be empty");
        }
        if (flightHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Flight hours cannot be negative");
        }
    }

    /**
     * Factory method para crear un dron desconocido automáticamente
     * cuando se recibe telemetría de un dron no registrado
     *
     * @param vehicleId ID del vehículo de la telemetría
     * @return Nueva instancia de Drone con valores predeterminados
     */
    public static Drone createUnknown(String vehicleId) {
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
        if (vehicleId.isBlank()) {
            throw new IllegalArgumentException("Vehicle ID cannot be empty");
        }

        LocalDateTime now = LocalDateTime.now();
        return new Drone(
                UUID.randomUUID().toString(),
                vehicleId,                          // Usar vehicleId como nombre
                vehicleId,
                "No Reconocida",                    // Marca no reconocida en español
                "Dron registrado automáticamente por telemetría",
                "00000000",                         // Serial number por defecto
                DroneStatus.ACTIVE,
                BigDecimal.ZERO,                    // Sin horas de vuelo
                now,
                now
        );
    }

    /**
     * Factory method para crear un dron con todos los datos
     */
    public static Drone create(
            String name,
            String vehicleId,
            String model,
            String description,
            String serialNumber,
            DroneStatus status
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Drone(
                UUID.randomUUID().toString(),
                name,
                vehicleId,
                model,
                description,
                serialNumber,
                status,
                BigDecimal.ZERO,
                now,
                now
        );
    }

    /**
     * Verifica si el dron es desconocido (creado automáticamente)
     */
    public boolean isUnknown() {
        return "No Reconocida".equals(model) && "00000000".equals(serialNumber);
    }

    /**
     * Actualiza las horas de vuelo
     */
    public Drone withFlightHours(BigDecimal newFlightHours) {
        Objects.requireNonNull(newFlightHours, "Flight hours cannot be null");
        if (newFlightHours.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Flight hours cannot be negative");
        }
        return new Drone(
                id, name, vehicleId, model, description, serialNumber, status,
                newFlightHours, createdAt, LocalDateTime.now()
        );
    }

    /**
     * Actualiza el estado del dron
     */
    public Drone withStatus(DroneStatus newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");
        return new Drone(
                id, name, vehicleId, model, description, serialNumber, newStatus,
                flightHours, createdAt, LocalDateTime.now()
        );
    }

    /**
     * Actualiza la información del dron (cuando se reconoce)
     */
    public Drone withDetails(String newName, String newModel, String newDescription, String newSerialNumber) {
        return new Drone(
                id, newName, vehicleId, newModel, newDescription, newSerialNumber, status,
                flightHours, createdAt, LocalDateTime.now()
        );
    }

}