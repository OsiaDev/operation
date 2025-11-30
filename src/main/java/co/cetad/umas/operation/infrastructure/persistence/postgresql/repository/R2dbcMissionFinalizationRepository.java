package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.MissionFinalizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para finalizaciones de misión
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcMissionFinalizationRepository extends JpaRepository<MissionFinalizationEntity, UUID> {

    /**
     * Busca una finalización por ID de misión
     * Útil para verificar si una misión ya fue finalizada
     */
    @Query("SELECT mf FROM MissionFinalizationEntity mf WHERE mf.missionId = :missionId")
    Optional<MissionFinalizationEntity> findByMissionId(@Param("missionId") UUID missionId);

}