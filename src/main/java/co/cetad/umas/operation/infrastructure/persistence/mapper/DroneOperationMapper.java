package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.DroneOperationEntity;
import co.cetad.umas.operation.domain.model.entity.OperationStatus;
import co.cetad.umas.operation.domain.model.vo.DroneOperation;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversión entre String (dominio) y UUID (persistencia)
 */
public final class DroneOperationMapper {

    private DroneOperationMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de dominio (String) a entidad de persistencia (UUID)
     */
    public static final Function<DroneOperation, DroneOperationEntity> toEntity = operation ->
            DroneOperationEntity.create(
                    parseUUID(operation.id()),
                    parseUUID(operation.droneId()),
                    parseUUIDNullable(operation.routeId()),
                    operation.status().name(),
                    operation.startDate(),
                    operation.createdAt(),
                    operation.updatedAt()
            );

    /**
     * Convierte de entidad de persistencia (UUID) a dominio (String)
     */
    public static final Function<DroneOperationEntity, DroneOperation> toDomain = entity ->
            new DroneOperation(
                    entity.id().toString(),
                    entity.droneId().toString(),
                    entity.routeId() != null ? entity.routeId().toString() : null,
                    parseOperationStatus(entity.status()),
                    entity.startDate(),
                    entity.createdAt(),
                    entity.updatedAt()
            );

    /**
     * Parsea String a UUID de forma segura
     * Lanza IllegalArgumentException si el String no es un UUID válido
     */
    private static UUID parseUUID(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new IllegalArgumentException("UUID string cannot be null or empty");
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }

    /**
     * Parsea String nullable a UUID
     * Retorna null si el String es null o vacío
     */
    private static UUID parseUUIDNullable(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }

    /**
     * Parsea el status de forma segura
     * Si el status no es válido, retorna PENDING por defecto
     */
    private static OperationStatus parseOperationStatus(String status) {
        return Optional.ofNullable(status)
                .map(String::toUpperCase)
                .map(s -> {
                    try {
                        return OperationStatus.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return OperationStatus.PENDING;
                    }
                })
                .orElse(OperationStatus.PENDING);
    }

}