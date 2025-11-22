package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio reactivo de Spring Data R2DBC para telemetría
 * Proporciona acceso asíncrono no bloqueante a PostgreSQL
 */
@Repository
public interface R2dbcDroneTelemetryRepository extends JpaRepository<DroneTelemetryEntity, String> {


}