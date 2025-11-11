package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetryEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repositorio reactivo de Spring Data R2DBC para telemetría
 * Proporciona acceso asíncrono no bloqueante a PostgreSQL
 */
@Repository
public interface R2dbcDroneTelemetryRepository extends R2dbcRepository<DroneTelemetryEntity, String> {

    /**
     * Encuentra la última telemetría de un vehículo
     */
    @Query("""
            SELECT * FROM drone_telemetry
            WHERE vehicle_id = :vehicleId
            ORDER BY timestamp DESC
            LIMIT 1
            """)
    Mono<DroneTelemetryEntity> findLatestByVehicleId(@Param("vehicleId") String vehicleId);

    /**
     * Encuentra telemetría en un rango de fechas
     */
    @Query("""
            SELECT * FROM drone_telemetry
            WHERE vehicle_id = :vehicleId
            AND timestamp BETWEEN :startDate AND :endDate
            ORDER BY timestamp DESC
            """)
    Flux<DroneTelemetryEntity> findByVehicleIdAndDateRange(
            @Param("vehicleId") String vehicleId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Encuentra las N últimas telemetrías de un vehículo
     */
    @Query("""
            SELECT * FROM drone_telemetry
            WHERE vehicle_id = :vehicleId
            ORDER BY timestamp DESC
            LIMIT :limit
            """)
    Flux<DroneTelemetryEntity> findRecentByVehicleId(
            @Param("vehicleId") String vehicleId,
            @Param("limit") int limit
    );

    /**
     * Elimina registros antiguos (para limpieza)
     */
    @Query("""
            DELETE FROM drone_telemetry
            WHERE created_at < :date
            """)
    Mono<Long> deleteOlderThan(@Param("date") LocalDateTime date);

}