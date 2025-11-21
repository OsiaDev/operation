package co.cetad.umas.operation.domain.model.entity;

/**
 * Estados posibles de una operación de dron
 * Representa el ciclo de vida de una operación desde su creación hasta su finalización
 */
public enum OperationStatus {
    /**
     * Operación creada pero aún no iniciada
     */
    PENDING,

    /**
     * Operación en progreso (dron ejecutando la misión)
     */
    IN_PROGRESS,

    /**
     * Operación completada exitosamente
     */
    COMPLETED,

    /**
     * Operación cancelada antes de completarse
     */
    CANCELLED,

    /**
     * Operación fallida por error o problema técnico
     */
    FAILED,

    /**
     * Operación suspendida temporalmente
     */
    SUSPENDED
}