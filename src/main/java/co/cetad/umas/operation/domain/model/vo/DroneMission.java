package co.cetad.umas.operation.domain.model.vo;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una misión de dron
 * Una misión es una orden de vuelo asignada a un dron específico
 *
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record DroneMission(
        String id,
        String name,
        String droneId,
        String routeId,
        String operatorId,
        MissionOrigin missionType,
        MissionState state,
        LocalDateTime startDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        Boolean isNew
) {

    /**
     * Constructor compacto con validaciones
     */
    public DroneMission {
        Objects.requireNonNull(id, "Mission ID cannot be null");
        Objects.requireNonNull(droneId, "Drone ID cannot be null");
        Objects.requireNonNull(operatorId, "Operator ID cannot be null");
        Objects.requireNonNull(missionType, "Mission type cannot be null");
        Objects.requireNonNull(state, "Mission state cannot be null");
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (droneId.isBlank()) {
            throw new IllegalArgumentException("Drone ID cannot be empty");
        }
        if (operatorId.isBlank()) {
            throw new IllegalArgumentException("Operator ID cannot be empty");
        }
    }

    /**
     * Factory method para crear una nueva misión MANUAL
     * Genera automáticamente el ID y las fechas de auditoría
     * Por defecto estado PENDIENTE_APROBACION y tipo MANUAL
     *
     * @param name Nombre de la misión (opcional)
     * @param droneId ID del dron asignado
     * @param routeId ID de la ruta (puede ser null)
     * @param operatorId ID del operador que crea la misión
     * @param startDate Fecha de inicio de la misión
     * @return Nueva instancia de DroneMission
     */
    public static DroneMission createManual(
            String name,
            String droneId,
            String routeId,
            String operatorId,
            LocalDateTime startDate
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new DroneMission(
                UUID.randomUUID().toString(),
                name,
                droneId,
                routeId,
                operatorId,
                MissionOrigin.MANUAL,
                MissionState.PENDIENTE_APROBACION,
                startDate,
                now,
                now,
                true
        );
    }

    /**
     * Factory method para crear una nueva misión AUTOMATICA
     * Usada cuando el dron vuela sin misión asignada
     *
     * @param droneId ID del dron
     * @param operatorId ID del operador/sistema
     * @param startDate Fecha de inicio
     * @return Nueva instancia de DroneMission automática
     */
    public static DroneMission createAutomatic(
            String droneId,
            String operatorId,
            LocalDateTime startDate
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new DroneMission(
                UUID.randomUUID().toString(),
                "Misión Automática",
                droneId,
                null,
                operatorId,
                MissionOrigin.AUTOMATICA,
                MissionState.EN_EJECUCION,
                startDate,
                now,
                now,
                true
        );
    }

    /**
     * Verifica si la misión tiene una ruta asignada
     */
    public boolean hasRoute() {
        return routeId != null && !routeId.isBlank();
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
        return startDate.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica si la misión ya debería haber comenzado
     */
    public boolean shouldHaveStarted() {
        return !startDate.isAfter(LocalDateTime.now());
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
     * Verifica si la misión está finalizada (cualquier estado terminal)
     */
    public boolean isFinished() {
        return state == MissionState.FINALIZADA ||
                state == MissionState.ABORTADA ||
                state == MissionState.FALLIDA ||
                state == MissionState.ARCHIVADA;
    }

    /**
     * Crea una copia de la misión con una nueva ruta asignada
     */
    public DroneMission withRoute(String newRouteId) {
        return new DroneMission(
                id, name, droneId, newRouteId, operatorId,
                missionType, state, startDate, createdAt,
                LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión con un nuevo nombre
     */
    public DroneMission withName(String newName) {
        return new DroneMission(
                id, newName, droneId, routeId, operatorId,
                missionType, state, startDate, createdAt,
                LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión con una nueva fecha de inicio
     */
    public DroneMission withStartDate(LocalDateTime newStartDate) {
        Objects.requireNonNull(newStartDate, "New start date cannot be null");
        return new DroneMission(
                id, name, droneId, routeId, operatorId,
                missionType, state, newStartDate, createdAt,
                LocalDateTime.now(), isNew
        );
    }

    /**
     * Crea una copia de la misión con un nuevo estado
     */
    public DroneMission withState(MissionState newState) {
        Objects.requireNonNull(newState, "New state cannot be null");
        return new DroneMission(
                id, name, droneId, routeId, operatorId,
                missionType, newState, startDate, createdAt,
                LocalDateTime.now(), isNew
        );
    }

}