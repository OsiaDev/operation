package co.cetad.umas.operation.infrastructure.persistence.adapter;

import co.cetad.umas.operation.domain.model.vo.KmlRoute;
import co.cetad.umas.operation.domain.ports.out.KmlRouteRepository;
import co.cetad.umas.operation.infrastructure.persistence.mapper.KmlRouteMapper;
import co.cetad.umas.operation.infrastructure.persistence.postgresql.repository.R2dbcKmlRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador de persistencia para rutas KML usando JPA
 * Mantiene la interfaz asíncrona con CompletableFuture
 *
 * Maneja conversiones String (dominio) ↔ UUID (repositorio)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmlRoutePersistenceAdapter implements KmlRouteRepository {

    private final R2dbcKmlRouteRepository repository;

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Optional<KmlRoute>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Searching for KML route with id: {}", id);

                Optional<KmlRoute> routeOpt = repository.findById(UUID.fromString(id))
                        .map(KmlRouteMapper.toDomain);

                if (routeOpt.isPresent()) {
                    log.debug("✅ Found KML route: {}", id);
                } else {
                    log.warn("⚠️ KML route not found: {}", id);
                }

                return routeOpt;
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid UUID format for route id: {}", id, e);
                return Optional.empty();
            } catch (Exception e) {
                log.error("❌ Error finding KML route by id: {}", id, e);
                return Optional.empty();
            }
        });
    }

}