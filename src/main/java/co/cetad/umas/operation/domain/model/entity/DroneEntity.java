package co.cetad.umas.operation.domain.model.entity;

import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("drone")
public record DroneEntity(
        UUID id,
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
