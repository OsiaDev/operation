package co.cetad.umas.operation.domain.ports.out;

import co.cetad.umas.operation.domain.model.vo.KmlRoute;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para persistencia de rutas KML
 * Define las operaciones sin acoplar al mecanismo de persistencia
 */
public interface KmlRouteRepository {

    /**
     * Busca una ruta KML por ID
     *
     * @param id ID de la ruta (String UUID)
     * @return Optional con la ruta si existe
     */
    CompletableFuture<Optional<KmlRoute>> findById(String id);

}