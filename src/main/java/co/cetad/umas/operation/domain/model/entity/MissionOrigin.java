package co.cetad.umas.operation.domain.model.entity;

/**
 * Origen de la misión
 * Define cómo se creó la misión
 */
public enum MissionOrigin {
    /**
     * Misión creada manualmente por usuario/comandante en la UI
     */
    MANUAL,

    /**
     * Misión creada automáticamente por telemetría/UGCS cuando dron vuela sin misión
     */
    AUTOMATICA
}