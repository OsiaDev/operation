package co.cetad.umas.operation.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una finalización de misión
 * Registra quién finalizó la misión y cuándo
 *
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record MissionFinalization(
        String id,
        String missionId,
        String commanderName,
        LocalDateTime createdAt,
        LocalDateTime decisionAt
) {

    /**
     * Constructor compacto con validaciones
     */
    public MissionFinalization {
        Objects.requireNonNull(id, "Mission finalization ID cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");
        Objects.requireNonNull(commanderName, "Commander name cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(decisionAt, "Decision at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Mission finalization ID cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (commanderName.isBlank()) {
            throw new IllegalArgumentException("Commander name cannot be empty");
        }
    }

    /**
     * Factory method para crear una nueva finalización de misión
     *
     * @param missionId ID de la misión a finalizar
     * @param commanderName Nombre del comandante que finaliza la misión
     * @return Nueva instancia de MissionFinalization
     */
    public static MissionFinalization create(String missionId, String commanderName) {
        LocalDateTime now = LocalDateTime.now();
        return new MissionFinalization(
                UUID.randomUUID().toString(),
                missionId,
                commanderName,
                now,
                now
        );
    }

}