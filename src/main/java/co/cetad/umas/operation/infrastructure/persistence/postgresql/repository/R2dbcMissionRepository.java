package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.MissionEntity;
import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para misiones
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission independiente de drones
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcMissionRepository extends JpaRepository<MissionEntity, UUID> {

    /**
     * Busca todas las misiones por tipo (MANUAL o AUTOMATICA)
     */
    @Query("SELECT m FROM MissionEntity m WHERE m.missionType = :missionType ORDER BY m.createdAt DESC")
    List<MissionEntity> findByMissionType(@Param("missionType") MissionOrigin missionType);

    /**
     * Busca todas las misiones por estado
     */
    @Query("SELECT m FROM MissionEntity m WHERE m.state = :state ORDER BY m.estimatedDate DESC")
    List<MissionEntity> findByState(@Param("state") MissionState state);

    /**
     * Busca todas las misiones pendientes de aprobación
     */
    @Query("SELECT m FROM MissionEntity m WHERE m.state = 'PENDIENTE_APROBACION' ORDER BY m.createdAt DESC")
    List<MissionEntity> findPendingApproval();

    /**
     * Busca todas las misiones programadas para una fecha específica
     */
    @Query("SELECT m FROM MissionEntity m WHERE DATE(m.estimatedDate) = DATE(:date) ORDER BY m.estimatedDate")
    List<MissionEntity> findByEstimatedDate(@Param("date") LocalDateTime date);

    /**
     * Busca todas las misiones en ejecución
     */
    @Query("SELECT m FROM MissionEntity m WHERE m.state = 'EN_EJECUCION' ORDER BY m.startDate DESC")
    List<MissionEntity> findInProgress();

    /**
     * Busca todas las misiones finalizadas
     */
    @Query("SELECT m FROM MissionEntity m " +
            "WHERE m.state IN ('FINALIZADA', 'ABORTADA', 'FALLIDA', 'ARCHIVADA') " +
            "ORDER BY m.endDate DESC")
    List<MissionEntity> findFinished();

}