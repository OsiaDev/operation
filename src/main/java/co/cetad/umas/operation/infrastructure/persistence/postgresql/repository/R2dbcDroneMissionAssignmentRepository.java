package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneMissionAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para asignaciones de drones a misiones
 *
 * REFACTORIZACIÓN: Nueva tabla para manejar relación muchos-a-muchos
 * entre misiones y drones
 *
 * IMPORTANTE: Usa UUID como tipo de ID porque la entidad usa UUID
 * El dominio usa String, pero la conversión se hace en el mapper
 */
@Repository
public interface R2dbcDroneMissionAssignmentRepository
        extends JpaRepository<DroneMissionAssignmentEntity, UUID> {

    /**
     * Busca todas las asignaciones de una misión específica
     * (todos los drones asignados a una misión)
     */
    @Query("SELECT dma FROM DroneMissionAssignmentEntity dma " +
            "WHERE dma.missionId = :missionId " +
            "ORDER BY dma.createdAt")
    List<DroneMissionAssignmentEntity> findByMissionId(@Param("missionId") UUID missionId);

    /**
     * Busca todas las asignaciones de un dron específico
     * (todas las misiones en las que está asignado un dron)
     */
    @Query("SELECT dma FROM DroneMissionAssignmentEntity dma " +
            "WHERE dma.droneId = :droneId " +
            "ORDER BY dma.createdAt DESC")
    List<DroneMissionAssignmentEntity> findByDroneId(@Param("droneId") UUID droneId);

    /**
     * Busca una asignación específica (misión + dron)
     * Útil para verificar si un dron ya está asignado a una misión
     */
    @Query("SELECT dma FROM DroneMissionAssignmentEntity dma " +
            "WHERE dma.missionId = :missionId AND dma.droneId = :droneId")
    Optional<DroneMissionAssignmentEntity> findByMissionIdAndDroneId(
            @Param("missionId") UUID missionId,
            @Param("droneId") UUID droneId
    );

    /**
     * Verifica si un dron ya está asignado a una misión
     */
    @Query("SELECT CASE WHEN COUNT(dma) > 0 THEN true ELSE false END " +
            "FROM DroneMissionAssignmentEntity dma " +
            "WHERE dma.missionId = :missionId AND dma.droneId = :droneId")
    boolean existsByMissionIdAndDroneId(
            @Param("missionId") UUID missionId,
            @Param("droneId") UUID droneId
    );

    /**
     * Cuenta cuántos drones están asignados a una misión
     */
    @Query("SELECT COUNT(dma) FROM DroneMissionAssignmentEntity dma " +
            "WHERE dma.missionId = :missionId")
    long countByMissionId(@Param("missionId") UUID missionId);

    /**
     * Busca todas las asignaciones que tienen una ruta específica
     */
    @Query("SELECT dma FROM DroneMissionAssignmentEntity dma " +
            "WHERE dma.routeId = :routeId " +
            "ORDER BY dma.createdAt DESC")
    List<DroneMissionAssignmentEntity> findByRouteId(@Param("routeId") UUID routeId);

}