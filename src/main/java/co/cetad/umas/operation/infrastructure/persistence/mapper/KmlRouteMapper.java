package co.cetad.umas.operation.infrastructure.persistence.mapper;

import co.cetad.umas.operation.domain.model.entity.KmlRouteEntity;
import co.cetad.umas.operation.domain.model.vo.KmlRoute;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper funcional entre el modelo de dominio y la entidad de persistencia
 * Mantiene la separación entre capas de arquitectura hexagonal
 *
 * Maneja conversiones:
 * - String (dominio) ↔ UUID (persistencia) para IDs
 */
public final class KmlRouteMapper {

    private KmlRouteMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Convierte de entidad de persistencia a dominio
     * UUID → String ID
     */
    public static final Function<KmlRouteEntity, KmlRoute> toDomain = entity ->
            new KmlRoute(
                    entity.getId().toString(),
                    entity.getName(),
                    entity.getStatus(),
                    entity.getOriginalFilename(),
                    entity.getKmlContent(),
                    entity.getGeojson(),
                    entity.getGeom(),
                    entity.getSizeBytes(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );

    /**
     * Convierte de dominio a entidad de persistencia
     * String ID → UUID
     */
    public static final Function<KmlRoute, KmlRouteEntity> toEntity = route -> {
        KmlRouteEntity entity = new KmlRouteEntity();

        entity.setId(UUID.fromString(route.id()));
        entity.setName(route.name());
        entity.setStatus(route.status());
        entity.setOriginalFilename(route.originalFilename());
        entity.setKmlContent(route.kmlContent());
        entity.setGeojson(route.geojson());
        entity.setGeom(route.geom());
        entity.setSizeBytes(route.sizeBytes());
        entity.setCreatedAt(route.createdAt());
        entity.setUpdatedAt(route.updatedAt());

        entity.setNew(true);

        return entity;
    };

}