package co.cetad.umas.operation.domain.model.entity;

import java.time.LocalDateTime;

public record OperatorEntity(
        String id,
        String username,
        String fullName,
        String email,
        String phoneNumber,
        String ugcsUserId,
        OperatorStatus status,
        Boolean isAvailable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}