package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneMissionEntity;
import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio reactivo R2DBC para misiones de drones
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcDroneMissionRepository extends JpaRepository<DroneMissionEntity, UUID> {

    /**
     * Busca todas las misiones por tipo (MANUAL o AUTOMATICA)
     */
    @Query("SELECT m FROM DroneMissionEntity m WHERE m.missionType = :missionType ORDER BY m.createdAt DESC")
    List<DroneMissionEntity> findByMissionType(@Param("missionType") MissionOrigin missionType);

}