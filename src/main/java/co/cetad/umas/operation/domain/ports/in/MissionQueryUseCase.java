package co.cetad.umas.operation.domain.ports.in;

import co.cetad.umas.operation.domain.model.vo.Mission;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para consultas de misiones (CQRS - Query Side)
 * Define operaciones de solo lectura sin modificar el estado
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission independiente de drones
 *
 * PATRÓN CQRS - QUERY SIDE:
 * - Retorna Optional<Mission> para permitir que el controller maneje "no encontrado"
 * - NO lanza excepciones de negocio (eso es responsabilidad del Command Side)
 * - Los errores técnicos se propagan como exceptionally() en CompletableFuture
 *
 * CONSISTENCIA:
 * - Mismo patrón que TelemetryQueryUseCase
 * - Delega al MissionRepository que también retorna Optional
 */
public interface MissionQueryUseCase {

    /**
     * Busca una misión por ID
     *
     * @param id ID de la misión (String UUID)
     * @return CompletableFuture con Optional<Mission>
     *         - Present: Si la misión existe
     *         - Empty: Si la misión no existe
     *         - Failed future: Si hay un error técnico (BD, etc.)
     */
    CompletableFuture<Optional<Mission>> findById(String id);

    /**
     * Busca todas las misiones
     *
     * @return CompletableFuture con lista de misiones (puede estar vacía)
     */
    CompletableFuture<List<Mission>> findAll();

    /**
     * Busca todas las misiones autorizadas (missionType = MANUAL)
     * Las misiones autorizadas son aquellas creadas manualmente por usuarios/comandantes
     *
     * @return CompletableFuture con lista de misiones MANUAL (puede estar vacía)
     */
    CompletableFuture<List<Mission>> findAuthorizedMissions();

    /**
     * Busca todas las misiones no autorizadas (missionType = AUTOMATICA)
     * Las misiones no autorizadas son aquellas creadas automáticamente por telemetría
     *
     * @return CompletableFuture con lista de misiones AUTOMATICA (puede estar vacía)
     */
    CompletableFuture<List<Mission>> findUnauthorizedMissions();

}