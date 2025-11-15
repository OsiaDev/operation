package co.cetad.umas.operation.domain.model.vo;

import java.util.Objects;

/**
 * Value Object simplificado que representa una ubicación geográfica
 * Solo validaciones técnicas básicas
 */
public record GeoLocation(
        double latitude,
        double longitude,
        Double altitude,
        Double accuracy
) {

    public GeoLocation {
        Objects.requireNonNull(latitude, "Latitude cannot be null");
        Objects.requireNonNull(longitude, "Longitude cannot be null");

        // Solo validaciones técnicas básicas de coordenadas
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    public static GeoLocation of(double latitude, double longitude) {
        return new GeoLocation(latitude, longitude, null, null);
    }

    public static GeoLocation withAltitude(double latitude, double longitude, double altitude) {
        return new GeoLocation(latitude, longitude, altitude, null);
    }

    /**
     * Calcula la distancia a otra ubicación usando la fórmula de Haversine
     * @return distancia en metros
     */
    public double distanceTo(GeoLocation other) {
        final double R = 6371e3; // Radio de la Tierra en metros
        final double φ1 = Math.toRadians(this.latitude);
        final double φ2 = Math.toRadians(other.latitude);
        final double Δφ = Math.toRadians(other.latitude - this.latitude);
        final double Δλ = Math.toRadians(other.longitude - this.longitude);

        final double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

}