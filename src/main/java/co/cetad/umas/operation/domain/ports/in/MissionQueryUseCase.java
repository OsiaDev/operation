package co.cetad.umas.operation.domain.ports.in;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de entrada para consultas de misiones (CQRS - Query Side)
 * Define operaciones de solo lectura sin modificar el estado
 */
public interface MissionQueryUseCase {

    /**
     * Busca una misión por ID
     */
    CompletableFuture<Optional<DroneMission>> findById(String id);

    /**
     * Busca todas las misiones
     */
    CompletableFuture<List<DroneMission>> findAll();

    /**
     * Busca todas las misiones autorizadas (missionType = MANUAL)
     * Las misiones autorizadas son aquellas creadas manualmente por usuarios/comandantes
     */
    CompletableFuture<List<DroneMission>> findAuthorizedMissions();

    /**
     * Busca todas las misiones no autorizadas (missionType = AUTOMATICA)
     * Las misiones no autorizadas son aquellas creadas automáticamente por telemetría
     */
    CompletableFuture<List<DroneMission>> findUnauthorizedMissions();

}