package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.MissionOrderEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio reactivo R2DBC para órdenes de misión
 */
@Repository
public interface R2dbcMissionOrderRepository extends R2dbcRepository<MissionOrderEntity, String> {

}