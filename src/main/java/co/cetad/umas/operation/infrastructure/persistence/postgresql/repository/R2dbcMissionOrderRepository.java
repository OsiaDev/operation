package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.MissionOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para órdenes de misión
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcMissionOrderRepository extends JpaRepository<MissionOrderEntity, UUID> {

    /**
     * Busca una orden por ID de misión
     * Útil para obtener quién creó una misión específica
     */
    @Query("SELECT mo FROM MissionOrderEntity mo WHERE mo.missionId = :missionId")
    Optional<MissionOrderEntity> findByMissionId(@Param("missionId") UUID missionId);

}