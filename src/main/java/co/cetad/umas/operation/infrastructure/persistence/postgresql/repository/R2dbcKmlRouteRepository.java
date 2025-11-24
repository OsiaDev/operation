package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.KmlRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio JPA para rutas KML
 * Proporciona acceso a PostgreSQL usando Spring Data JPA
 */
@Repository
public interface R2dbcKmlRouteRepository extends JpaRepository<KmlRouteEntity, UUID> {

}