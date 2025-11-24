package co.cetad.umas.operation.domain.model.vo;

import co.cetad.umas.operation.domain.model.entity.RouteStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio que representa una ruta KML
 * Usa String para IDs para mantener independencia de la capa de persistencia
 */
public record KmlRoute(
        String id,
        String name,
        RouteStatus status,
        String originalFilename,
        String kmlContent,
        String geojson,
        String geom,
        Long sizeBytes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * Constructor compacto con validaciones
     */
    public KmlRoute {
        Objects.requireNonNull(id, "Route ID cannot be null");
        Objects.requireNonNull(name, "Route name cannot be null");
        Objects.requireNonNull(status, "Route status cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("Route ID cannot be empty");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("Route name cannot be empty");
        }
    }

    /**
     * Verifica si la ruta tiene contenido GeoJSON
     */
    public boolean hasGeoJson() {
        return geojson != null && !geojson.isBlank();
    }

    /**
     * Verifica si la ruta está activa
     */
    public boolean isActive() {
        return status == RouteStatus.ACTIVE;
    }

}