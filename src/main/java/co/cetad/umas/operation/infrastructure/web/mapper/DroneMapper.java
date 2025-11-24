package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneEntity;
import co.cetad.umas.operation.domain.model.vo.Drone;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones:
 * - String (dominio) ↔ UUID (persistencia) para IDs
 */
public final class DroneMapper {

    private DroneMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio a entidad de persistencia
     * String ID → UUID
     */
    public static final Function<Drone, DroneEntity> toEntity = drone -> {
        DroneEntity entity = new DroneEntity();

        entity.setId(UUID.fromString(drone.id()));
        entity.setName(drone.name());
        entity.setVehicleId(drone.vehicleId());
        entity.setModel(drone.model());
        entity.setDescription(drone.description());
        entity.setSerialNumber(drone.serialNumber());
        entity.setStatus(drone.status());
        entity.setFlightHours(drone.flightHours());
        entity.setCreatedAt(drone.createdAt());
        entity.setUpdatedAt(drone.updatedAt());

        // Marcar como nuevo para INSERT
        entity.setNew(true);

        return entity;
    };

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID → String ID
     */
    public static final Function<DroneEntity, Drone> toDomain = entity ->
            new Drone(
                    entity.getId().toString(),
                    entity.getName(),
                    entity.getVehicleId(),
                    entity.getModel(),
                    entity.getDescription(),
                    entity.getSerialNumber(),
                    entity.getStatus(),
                    entity.getFlightHours(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );

}