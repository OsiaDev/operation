package co.cetad.umas.operation.domain.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DroneEntity(
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
}
