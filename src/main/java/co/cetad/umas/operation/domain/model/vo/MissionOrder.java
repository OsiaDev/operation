package co.cetad.umas.operation.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una orden de misión
 * Registra quién creó/aprobó la misión
 *
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record MissionOrder(
        String id,
        String missionId,
        String commanderName,
        LocalDateTime createdAt,
        LocalDateTime decisionAt
) {

    /**
     * Constructor compacto con validaciones
     */
    public MissionOrder {
        Objects.requireNonNull(id, "Mission order ID cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(commanderName, "Commander name cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Mission order ID cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (commanderName.isBlank()) {
            throw new IllegalArgumentException("Commander name cannot be empty");
        }
    }

    /**
     * Factory method para crear una nueva orden de misión
     *
     * @param missionId ID de la misión
     * @param commanderName Nombre del comandante que crea la misión
     * @return Nueva instancia de MissionOrder
     */
    public static MissionOrder create(String missionId, String commanderName) {
        return new MissionOrder(
                UUID.randomUUID().toString(),
                missionId,
                commanderName,
                LocalDateTime.now(),
                null
        );
    }

    /**
     * Crea una copia de la orden con fecha de decisión
     */
    public MissionOrder withDecision(LocalDateTime decisionAt) {
        Objects.requireNonNull(decisionAt, "Decision date cannot be null");
        return new MissionOrder(
                id,
                missionId,
                commanderName,
                createdAt,
                decisionAt
        );
    }

    /**
     * Verifica si la orden tiene fecha de decisión
     */
    public boolean hasDecision() {
        return decisionAt != null;
    }

}