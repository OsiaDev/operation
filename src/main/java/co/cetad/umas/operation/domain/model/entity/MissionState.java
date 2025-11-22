package co.cetad.umas.operation.domain.model.entity;

/**
 * Estados del ciclo de vida de una misión
 */
public enum MissionState {
    /**
     * Misión creada, pendiente de aprobación
     */
    PENDIENTE_APROBACION,

    /**
     * Misión aprobada, lista para ejecución
     */
    APROBADA,

    /**
     * Misión en ejecución activa
     */
    EN_EJECUCION,

    /**
     * Misión pausada temporalmente
     */
    PAUSADA,

    /**
     * Misión finalizada exitosamente
     */
    FINALIZADA,

    /**
     * Misión abortada por decisión operativa
     */
    ABORTADA,

    /**
     * Misión fallida por error técnico
     */
    FALLIDA,

    /**
     * Misión archivada
     */
    ARCHIVADA
}