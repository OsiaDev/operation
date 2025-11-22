package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneMissionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio reactivo R2DBC para misiones de drones
 */
@Repository
public interface R2dbcDroneMissionRepository extends R2dbcRepository<DroneMissionEntity, String> {

}