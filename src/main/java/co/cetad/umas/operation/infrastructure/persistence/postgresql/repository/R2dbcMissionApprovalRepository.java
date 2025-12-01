package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.MissionApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para aprobaciones de misión
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcMissionApprovalRepository extends JpaRepository<MissionApprovalEntity, UUID> {

    /**
     * Busca una aprobación por ID de misión
     * Útil para obtener quién aprobó una misión específica
     */
    @Query("SELECT ma FROM MissionApprovalEntity ma WHERE ma.missionId = :missionId")
    Optional<MissionApprovalEntity> findByMissionId(@Param("missionId") UUID missionId);

}