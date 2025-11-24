package co.cetad.umas.operation.infrastructure.persistence.postgresql.repository;

import co.cetad.umas.operation.domain.model.entity.DroneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para drones
 * Proporciona acceso a PostgreSQL usando Spring Data JPA
 */
@Repository
public interface R2dbcDroneRepository extends JpaRepository<DroneEntity, UUID> {

    /**
     * Busca un dron por vehicle ID
     */
    @Query("SELECT d FROM DroneEntity d WHERE d.vehicleId = :vehicleId")
    Optional<DroneEntity> findByVehicleId(@Param("vehicleId") String vehicleId);

    /**
     * Verifica si existe un dron con el vehicle ID dado
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DroneEntity d WHERE d.vehicleId = :vehicleId")
    boolean existsByVehicleId(@Param("vehicleId") String vehicleId);

}