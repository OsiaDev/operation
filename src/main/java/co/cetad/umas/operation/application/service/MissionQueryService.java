package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.vo.Mission;
import co.cetad.umas.operation.domain.ports.in.MissionQueryUseCase;
import co.cetad.umas.operation.domain.ports.out.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de consultas de misiones (CQRS - Query Side)
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission independiente de drones
 *
 * Responsabilidad única:
 * - Coordinar consultas de misiones
 * - Aplicar lógica de validación de parámetros
 * - Delegar al repository
 *
 * Sin efectos secundarios, solo lectura
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionQueryService implements MissionQueryUseCase {

    private final MissionRepository missionRepository;

    @Override
    public CompletableFuture<Optional<Mission>> findById(String id) {
        log.debug("Querying mission by id: {}", id);

        return validateId(id)
                .thenCompose(missionRepository::findById)
                .exceptionally(throwable -> {
                    log.error("Error querying mission by id: {}", id, throwable);
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<List<Mission>> findAll() {
        log.debug("Querying all missions");

        return missionRepository.findAll()
                .exceptionally(throwable -> {
                    log.error("Error querying all missions", throwable);
                    return List.of();
                });
    }

    @Override
    public CompletableFuture<List<Mission>> findAuthorizedMissions() {
        log.debug("Querying authorized missions (MANUAL)");

        return missionRepository.findByMissionType(MissionOrigin.MANUAL)
                .thenApply(missions -> {
                    log.info("Found {} authorized missions", missions.size());
                    return missions;
                })
                .exceptionally(throwable -> {
                    log.error("Error querying authorized missions", throwable);
                    return List.of();
                });
    }

    @Override
    public CompletableFuture<List<Mission>> findUnauthorizedMissions() {
        log.debug("Querying unauthorized missions (AUTOMATICA)");

        return missionRepository.findByMissionType(MissionOrigin.AUTOMATICA)
                .thenApply(missions -> {
                    log.info("Found {} unauthorized missions", missions.size());
                    return missions;
                })
                .exceptionally(throwable -> {
                    log.error("Error querying unauthorized missions", throwable);
                    return List.of();
                });
    }

    /**
     * Valida el ID
     */
    private CompletableFuture<String> validateId(String id) {
        return CompletableFuture.supplyAsync(() -> {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("ID cannot be null or empty");
            }
            return id;
        });
    }

}