package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.vo.Waypoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para parsear GeoJSON y extraer waypoints
 *
 * Soporta:
 * - FeatureCollection con LineString
 * - Feature con LineString
 * - LineString directamente
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoJsonParserService {

    private final ObjectMapper objectMapper;

    /**
     * Extrae waypoints desde un GeoJSON string
     *
     * @param geoJson String JSON con formato GeoJSON
     * @return Lista de waypoints extraídos
     * @throws GeoJsonParseException si el formato es inválido
     */
    public List<Waypoint> extractWaypoints(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            throw new GeoJsonParseException("GeoJSON cannot be null or empty");
        }

        try {
            log.debug("Parsing GeoJSON to extract waypoints");

            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.path("type").asText();

            return switch (type) {
                case "FeatureCollection" -> extractFromFeatureCollection(root);
                case "Feature" -> extractFromFeature(root);
                case "LineString" -> extractFromLineString(root);
                default -> throw new GeoJsonParseException(
                        "Unsupported GeoJSON type: " + type + ". Supported types: FeatureCollection, Feature, LineString"
                );
            };

        } catch (GeoJsonParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error parsing GeoJSON", e);
            throw new GeoJsonParseException("Failed to parse GeoJSON: " + e.getMessage(), e);
        }
    }

    /**
     * Extrae coordenadas desde un FeatureCollection
     */
    private List<Waypoint> extractFromFeatureCollection(JsonNode root) {
        JsonNode features = root.path("features");

        if (!features.isArray() || features.isEmpty()) {
            throw new GeoJsonParseException("FeatureCollection must have at least one feature");
        }

        // Procesar la primera feature (asumimos que contiene la ruta principal)
        JsonNode firstFeature = features.get(0);
        return extractFromFeature(firstFeature);
    }

    /**
     * Extrae coordenadas desde un Feature
     */
    private List<Waypoint> extractFromFeature(JsonNode feature) {
        JsonNode geometry = feature.path("geometry");

        if (geometry.isMissingNode()) {
            throw new GeoJsonParseException("Feature must have a geometry");
        }

        return extractFromGeometry(geometry);
    }

    /**
     * Extrae coordenadas desde una geometría
     */
    private List<Waypoint> extractFromGeometry(JsonNode geometry) {
        String geometryType = geometry.path("type").asText();

        return switch (geometryType) {
            case "LineString" -> extractFromLineString(geometry);
            case "MultiLineString" -> extractFromMultiLineString(geometry);
            default -> throw new GeoJsonParseException(
                    "Unsupported geometry type: " + geometryType + ". Supported types: LineString, MultiLineString"
            );
        };
    }

    /**
     * Extrae coordenadas desde un LineString
     * GeoJSON LineString format: { "type": "LineString", "coordinates": [[lon, lat], [lon, lat], ...] }
     */
    private List<Waypoint> extractFromLineString(JsonNode lineString) {
        JsonNode coordinates = lineString.path("coordinates");

        if (!coordinates.isArray() || coordinates.isEmpty()) {
            throw new GeoJsonParseException("LineString must have at least one coordinate");
        }

        List<Waypoint> waypoints = new ArrayList<>();

        for (JsonNode coordinate : coordinates) {
            if (!coordinate.isArray() || coordinate.size() < 2) {
                log.warn("⚠️ Skipping invalid coordinate: {}", coordinate);
                continue;
            }

            double longitude = coordinate.get(0).asDouble();
            double latitude = coordinate.get(1).asDouble();

            // Crear waypoint (constructor toma latitude, longitude)
            waypoints.add(new Waypoint(latitude, longitude));
        }

        if (waypoints.isEmpty()) {
            throw new GeoJsonParseException("No valid coordinates found in LineString");
        }

        log.info("✅ Extracted {} waypoints from GeoJSON", waypoints.size());
        return waypoints;
    }

    /**
     * Extrae coordenadas desde un MultiLineString
     * Concatena todas las líneas en una sola lista de waypoints
     */
    private List<Waypoint> extractFromMultiLineString(JsonNode multiLineString) {
        JsonNode coordinates = multiLineString.path("coordinates");

        if (!coordinates.isArray() || coordinates.isEmpty()) {
            throw new GeoJsonParseException("MultiLineString must have at least one LineString");
        }

        List<Waypoint> waypoints = new ArrayList<>();

        for (JsonNode lineStringCoords : coordinates) {
            if (!lineStringCoords.isArray()) {
                log.warn("⚠️ Skipping invalid LineString in MultiLineString");
                continue;
            }

            for (JsonNode coordinate : lineStringCoords) {
                if (!coordinate.isArray() || coordinate.size() < 2) {
                    log.warn("⚠️ Skipping invalid coordinate: {}", coordinate);
                    continue;
                }

                double longitude = coordinate.get(0).asDouble();
                double latitude = coordinate.get(1).asDouble();

                waypoints.add(new Waypoint(latitude, longitude));
            }
        }

        if (waypoints.isEmpty()) {
            throw new GeoJsonParseException("No valid coordinates found in MultiLineString");
        }

        log.info("✅ Extracted {} waypoints from MultiLineString", waypoints.size());
        return waypoints;
    }

    /**
     * Excepción personalizada para errores de parsing de GeoJSON
     */
    public static class GeoJsonParseException extends RuntimeException {
        public GeoJsonParseException(String message) {
            super(message);
        }

        public GeoJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}