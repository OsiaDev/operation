package co.cetad.umas.operation.domain.model.vo;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una MISIÓN general
 *
 * REFACTORIZACIÓN: Ahora una misión es independiente de los drones
 * Los drones se asignan a través de DroneMissionAssignment
 *
 * Una misión puede tener múltiples drones asignados
 *
 * FECHAS:
 * - estimatedDate: Fecha estimada de ejecución (NOT NULL - siempre existe)
 * - startDate: Fecha real de inicio (NULL hasta que inicie)
 * - endDate: Fecha real de finalización (NULL hasta que termine)
 *
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record Mission(
        String id,
        String name,
        String operatorId,
        MissionOrigin missionType,
        MissionState state,
        LocalDateTime estimatedDate,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isNew
) {

    /**
     * Constructor compacto con validaciones
     */
    public Mission {
        Objects.requireNonNull(id, "Mission ID cannot be null");
        Objects.requireNonNull(operatorId, "Operator ID cannot be null");
        Objects.requireNonNull(missionType, "Mission type cannot be null");
        Objects.requireNonNull(state, "Mission state cannot be null");
        Objects.requireNonNull(estimatedDate, "Estimated date cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (operatorId.isBlank()) {
            throw new IllegalArgumentException("Operator ID cannot be empty");
        }

        // Validar fechas
        if (startDate != null && startDate.isBefore(estimatedDate)) {
            throw new IllegalArgumentException("Start date cannot be before estimated date");
        }
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    /**
     * Factory method para crear una nueva misión MANUAL
     * Por defecto estado PENDIENTE_APROBACION y tipo MANUAL
     *
     * @param name Nombre de la misión (opcional)
     * @param operatorId ID del operador que crea la misión
     * @param estimatedDate Fecha estimada de ejecución
     * @return Nueva instancia de Mission
     */
    public static Mission createManual(
            String name,
            String operatorId,
            LocalDateTime estimatedDate
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Mission(
                UUID.randomUUID().toString(),
                name,
                operatorId,
                MissionOrigin.MANUAL,
                MissionState.PENDIENTE_APROBACION,
                estimatedDate,
                null,  // No ha iniciado aún
                null,  // No ha terminado aún
                now,
                now,
                true
        );
    }

    /**
     * Factory method para crear una nueva misión AUTOMATICA
     * Usada cuando el dron vuela sin misión asignada
     *
     * @param operatorId ID del operador/sistema
     * @param estimatedDate Fecha estimada de ejecución
     * @return Nueva instancia de Mission automática
     */
    public static Mission createAutomatic(
            String operatorId,
            LocalDateTime estimatedDate
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Mission(
                UUID.randomUUID().toString(),
                "Misión Automática",
                operatorId,
                MissionOrigin.AUTOMATICA,
                MissionState.EN_EJECUCION,  // Automáticas inician directo
                estimatedDate,
                now,  // Inicia inmediatamente
                null,  // No ha terminado aún
                now,
                now,
                true
        );
    }

    /**
     * Verifica si la misión tiene nombre
     */
    public boolean hasName() {
        return name != null && !name.isBlank();
    }

    /**
     * Verifica si la misión está programada para el futuro
     */
    public boolean isScheduledForFuture() {
        return estimatedDate.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si la misión ya debería haber comenzado
     */
    public boolean shouldHaveStarted() {
        return !estimatedDate.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si la misión es manual
     */
    public boolean isManual() {
        return missionType == MissionOrigin.MANUAL;
    }

    /**
     * Verifica si la misión es automática
     */
    public boolean isAutomatic() {
        return missionType == MissionOrigin.AUTOMATICA;
    }

    /**
     * Verifica si la misión está pendiente de aprobación
     */
    public boolean isPendingApproval() {
        return state == MissionState.PENDIENTE_APROBACION;
    }

    /**
     * Verifica si la misión está aprobada
     */
    public boolean isApproved() {
        return state == MissionState.APROBADA;
    }

    /**
     * Verifica si la misión está en ejecución
     */
    public boolean isInProgress() {
        return state == MissionState.EN_EJECUCION;
    }

    /**
     * Verifica si la misión ha iniciado (tiene startDate)
     */
    public boolean hasStarted() {
        return startDate != null;
    }

    /**
     * Verifica si la misión ha terminado (tiene endDate)
     */
    public boolean hasEnded() {
        return endDate != null;
    }

    /**
     * Verifica si la misión está finalizada (cualquier estado terminal)
     */
    public boolean isFinished() {
        return state == MissionState.FINALIZADA ||
                state == MissionState.ABORTADA ||
                state == MissionState.FALLIDA ||
                state == MissionState.ARCHIVADA;
    }

    /**
     * Crea una copia de la misión con un nuevo nombre
     */
    public Mission withName(String newName) {
        return new Mission(
                id, newName, operatorId, missionType, state,
                estimatedDate, startDate, endDate,
                createdAt, LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión con una nueva fecha estimada
     */
    public Mission withEstimatedDate(LocalDateTime newEstimatedDate) {
        Objects.requireNonNull(newEstimatedDate, "New estimated date cannot be null");
        return new Mission(
                id, name, operatorId, missionType, state,
                newEstimatedDate, startDate, endDate,
                createdAt, LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión con un nuevo estado
     */
    public Mission withState(MissionState newState) {
        Objects.requireNonNull(newState, "New state cannot be null");
        return new Mission(
                id, name, operatorId, missionType, newState,
                estimatedDate, startDate, endDate,
                createdAt, LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión marcando el inicio de ejecución
     */
    public Mission withStarted(LocalDateTime startTime) {
        Objects.requireNonNull(startTime, "Start time cannot be null");
        return new Mission(
                id, name, operatorId, missionType, MissionState.EN_EJECUCION,
                estimatedDate, startTime, endDate,
                createdAt, LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión marcando el fin de ejecución
     */
    public Mission withEnded(LocalDateTime endTime, MissionState finalState) {
        Objects.requireNonNull(endTime, "End time cannot be null");
        Objects.requireNonNull(finalState, "Final state cannot be null");

        if (!finalState.equals(MissionState.FINALIZADA) &&
                !finalState.equals(MissionState.ABORTADA) &&
                !finalState.equals(MissionState.FALLIDA)) {
            throw new IllegalArgumentException(
                    "Final state must be FINALIZADA, ABORTADA, or FALLIDA");
        }

        return new Mission(
                id, name, operatorId, missionType, finalState,
                estimatedDate, startDate, endTime,
                createdAt, LocalDateTime.now(), isNew
        );
    }

}