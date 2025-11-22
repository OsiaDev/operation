package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de misiones de drones
 */
public interface DroneMissionRepository {

    /**
     * Guarda una misión de dron
     */
    CompletableFuture<DroneMission> save(DroneMission mission);

    /**
     * Busca una misión por ID
     */
    CompletableFuture<Optional<DroneMission>> findById(String id);

    /**
     * Busca todas las misiones
     */
    CompletableFuture<List<DroneMission>> findAll();

    /**
     * Busca todas las misiones por tipo (MANUAL o AUTOMATICA)
     */
    CompletableFuture<List<DroneMission>> findByMissionType(MissionOrigin missionType);

}