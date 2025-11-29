package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import co.cetad.umas.operation.domain.model.vo.Mission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de misiones
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission independiente de drones
 */
public interface MissionRepository {

    /**
     * Guarda una misión
     */
    CompletableFuture<Mission> save(Mission mission);

    /**
     * Busca una misión por ID
     */
    CompletableFuture<Optional<Mission>> findById(String id);

    /**
     * Busca todas las misiones
     */
    CompletableFuture<List<Mission>> findAll();

    /**
     * Busca todas las misiones por tipo (MANUAL o AUTOMATICA)
     */
    CompletableFuture<List<Mission>> findByMissionType(MissionOrigin missionType);

    /**
     * Busca todas las misiones por estado
     */
    CompletableFuture<List<Mission>> findByState(MissionState state);

    /**
     * Busca todas las misiones pendientes de aprobación
     */
    CompletableFuture<List<Mission>> findPendingApproval();

    /**
     * Busca todas las misiones programadas para una fecha específica
     */
    CompletableFuture<List<Mission>> findByEstimatedDate(LocalDateTime date);

    /**
     * Busca todas las misiones en ejecución
     */
    CompletableFuture<List<Mission>> findInProgress();

    /**
     * Busca todas las misiones finalizadas
     */
    CompletableFuture<List<Mission>> findFinished();

}