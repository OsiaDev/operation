package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.MissionExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para ejecuciones de misión
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcMissionExecutionRepository extends JpaRepository<MissionExecutionEntity, UUID> {

    /**
     * Busca una ejecución por ID de misión
     * Útil para obtener quién ejecutó una misión específica
     */
    @Query("SELECT me FROM MissionExecutionEntity me WHERE me.missionId = :missionId")
    Optional<MissionExecutionEntity> findByMissionId(@Param("missionId") UUID missionId);

}