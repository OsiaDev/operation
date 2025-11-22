package co.cetad.umas.operation.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una aprobación de misión
 * Registra quién aprobó la misión y cuándo
 *
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record MissionApproval(
        String id,
        String missionId,
        String commanderName,
        LocalDateTime createdAt,
        LocalDateTime decisionAt
) {

    /**
     * Constructor compacto con validaciones
     */
    public MissionApproval {
        Objects.requireNonNull(id, "Mission approval ID cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(commanderName, "Commander name cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(decisionAt, "Decision at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Mission approval ID cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (commanderName.isBlank()) {
            throw new IllegalArgumentException("Commander name cannot be empty");
        }
    }

    /**
     * Factory method para crear una nueva aprobación de misión
     *
     * @param missionId ID de la misión a aprobar
     * @param commanderName Nombre del comandante que aprueba la misión
     * @return Nueva instancia de MissionApproval
     */
    public static MissionApproval create(String missionId, String commanderName) {
        LocalDateTime now = LocalDateTime.now();
        return new MissionApproval(
                UUID.randomUUID().toString(),
                missionId,
                commanderName,
                now,
                now
        );
    }

}