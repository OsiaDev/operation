package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para telemetría de drones
 * Proporciona acceso a PostgreSQL usando Spring Data JPA
 */
@Repository
public interface R2dbcDroneTelemetryRepository extends JpaRepository<DroneTelemetryEntity, UUID> {

    /**
     * Busca la última telemetría de un vehículo ordenada por timestamp
     */
    @Query("SELECT t FROM DroneTelemetryEntity t " +
            "WHERE t.vehicleId = :vehicleId " +
            "ORDER BY t.timestamp DESC " +
            "LIMIT 1")
    Optional<DroneTelemetryEntity> findLatestByVehicleId(@Param("vehicleId") String vehicleId);

    /**
     * Busca telemetría de un vehículo en un rango de fechas
     */
    @Query("SELECT t FROM DroneTelemetryEntity t " +
            "WHERE t.vehicleId = :vehicleId " +
            "AND t.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY t.timestamp DESC")
    List<DroneTelemetryEntity> findByVehicleIdAndDateRange(
            @Param("vehicleId") String vehicleId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca telemetría reciente de un vehículo (últimas N entradas)
     */
    @Query("SELECT t FROM DroneTelemetryEntity t " +
            "WHERE t.vehicleId = :vehicleId " +
            "ORDER BY t.timestamp DESC " +
            "LIMIT :limit")
    List<DroneTelemetryEntity> findRecentByVehicleId(
            @Param("vehicleId") String vehicleId,
            @Param("limit") int limit
    );

    /**
     * Elimina telemetría antigua (para limpieza)
     */
    @Modifying
    @Query("DELETE FROM DroneTelemetryEntity t WHERE t.timestamp < :date")
    long deleteOlderThan(@Param("date") LocalDateTime date);

}