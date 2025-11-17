package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneOperationEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio reactivo R2DBC para operaciones
 */
@Repository
public interface R2dbcDroneOperationRepository extends R2dbcRepository<DroneOperationEntity, String> {

}