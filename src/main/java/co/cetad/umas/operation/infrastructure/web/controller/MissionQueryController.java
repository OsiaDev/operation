package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.HealthResponse;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.ports.in.MissionQueryUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionAssignmentRepository;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.infrastructure.web.mapper.MissionResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller REST para consultas de misiones de drones
 *
 * REFACTORIZACIÓN: Ahora carga información completa de drones en una sola query
 * Previene el problema N+1 queries usando batch loading
 *
 * Endpoints:
 * - GET /api/v1/missions                    - Listar todas las misiones
 * - GET /api/v1/missions/{id}               - Buscar misión por ID
 * - GET /api/v1/missions/authorized         - Listar misiones autorizadas (MANUAL)
 * - GET /api/v1/missions/unauthorized       - Listar misiones no autorizadas (AUTOMATICA)
 * - GET /api/v1/missions/health             - Health check
 *
 * PATRÓN CQRS - QUERY SIDE:
 * - Usa MissionQueryUseCase que retorna Optional
 * - El controller decide el HTTP status (404, 200, etc.)
 * - Maneja errores de validación con HTTP 400
 * - Carga asignaciones de drones Y detalles completos de cada dron
 *
 * OPTIMIZACIÓN:
 * - Para evitar N+1 queries, carga todos los drones en batch
 * - Usa Map<droneId, Drone> para lookup O(1)
 *
 * Usa WebFlux para respuestas reactivas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionQueryController {

    private final MissionQueryUseCase missionQueryUseCase;
    private final DroneMissionAssignmentRepository assignmentRepository;
    private final DroneRepository droneRepository;

    /**
     * Lista todas las misiones con sus drones asignados (información completa)
     *
     * GET /api/v1/missions
     *
     * Response: 200 OK con lista de misiones (puede estar vacía)
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MissionResponse> findAll() {
        log.info("GET /api/v1/missions - Listing all missions");

        return Mono.fromFuture(missionQueryUseCase.findAll())
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::loadMissionWithDroneDetails)
                .doOnComplete(() -> log.info("✅ Completed listing all missions"))
                .doOnError(error -> {
                    log.error("❌ Error listing all missions", error);
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error listing missions: " + error.getMessage(),
                            error
                    );
                });
    }

    /**
     * Busca una misión por ID con sus drones asignados (información completa)
     *
     * GET /api/v1/missions/{id}
     *
     * Response:
     * - 200 OK: Misión encontrada
     * - 404 NOT FOUND: Misión no existe
     * - 400 BAD REQUEST: ID inválido
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MissionResponse>> findById(
            @PathVariable String id
    ) {
        log.info("GET /api/v1/missions/{}", id);

        return Mono.fromFuture(missionQueryUseCase.findById(id))
                .flatMap(missionOpt -> {
                    if (missionOpt.isEmpty()) {
                        log.warn("⚠️ Mission not found: {}", id);
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Mission not found with id: " + id
                        );
                    }

                    var mission = missionOpt.get();
                    return loadMissionWithDroneDetails(mission)
                            .map(ResponseEntity::ok);
                })
                .doOnSuccess(response ->
                        log.info("✅ Found mission with id: {}", id))
                .onErrorResume(ResponseStatusException.class, Mono::error)
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.warn("⚠️ Invalid ID format: {}", id, error);
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid mission ID format: " + id,
                            error
                    );
                })
                .doOnError(error -> {
                    if (!(error instanceof ResponseStatusException)) {
                        log.error("❌ Error finding mission with id: {}", id, error);
                    }
                });
    }

    /**
     * Lista todas las misiones autorizadas (creadas manualmente)
     *
     * GET /api/v1/missions/authorized
     *
     * Retorna misiones con missionType = MANUAL
     * Estas son misiones creadas por usuarios/comandantes en la interfaz
     *
     * Response: 200 OK con lista de misiones MANUAL (puede estar vacía)
     */
    @GetMapping(value = "/authorized", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MissionResponse> findAuthorizedMissions() {
        log.info("GET /api/v1/missions/authorized - Listing authorized missions");

        return Mono.fromFuture(missionQueryUseCase.findAuthorizedMissions())
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::loadMissionWithDroneDetails)
                .doOnComplete(() -> log.info("✅ Completed listing authorized missions"))
                .doOnError(error -> {
                    log.error("❌ Error listing authorized missions", error);
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error listing authorized missions: " + error.getMessage(),
                            error
                    );
                });
    }

    /**
     * Lista todas las misiones no autorizadas (creadas automáticamente)
     *
     * GET /api/v1/missions/unauthorized
     *
     * Retorna misiones con missionType = AUTOMATICA
     * Estas son misiones creadas automáticamente por telemetría cuando
     * un dron vuela sin misión asignada
     *
     * Response: 200 OK con lista de misiones AUTOMATICA (puede estar vacía)
     */
    @GetMapping(value = "/unauthorized", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MissionResponse> findUnauthorizedMissions() {
        log.info("GET /api/v1/missions/unauthorized - Listing unauthorized missions");

        return Mono.fromFuture(missionQueryUseCase.findUnauthorizedMissions())
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::loadMissionWithDroneDetails)
                .doOnComplete(() -> log.info("✅ Completed listing unauthorized missions"))
                .doOnError(error -> {
                    log.error("❌ Error listing unauthorized missions", error);
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error listing unauthorized missions: " + error.getMessage(),
                            error
                    );
                });
    }

    /**
     * Health check del controller
     *
     * GET /api/v1/missions/health
     *
     * Response: 200 OK siempre
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<HealthResponse>> health() {
        return Mono.just(ResponseEntity.ok(
                new HealthResponse("UP", "Mission Query Controller is running")
        ));
    }

    /**
     * Carga una misión con información completa de drones
     * Previene N+1 queries cargando todos los drones en batch
     *
     * OPTIMIZACIÓN:
     * 1. Cargar asignaciones de la misión
     * 2. Extraer IDs de drones únicos
     * 3. Cargar TODOS los drones en batch (evita N+1 queries)
     * 4. Crear Map para lookup O(1)
     * 5. Mapear a response
     */
    private Mono<MissionResponse> loadMissionWithDroneDetails(
            co.cetad.umas.operation.domain.model.vo.Mission mission
    ) {
        return Mono.fromFuture(assignmentRepository.findByMissionId(mission.id()))
                .flatMap(assignments -> {
                    if (assignments.isEmpty()) {
                        // Sin drones asignados, retornar response simple
                        return Mono.just(
                                MissionResponseMapper.toResponseWithoutAssignments(mission)
                        );
                    }

                    // Extraer IDs únicos de drones
                    List<String> droneIds = assignments.stream()
                            .map(DroneMissionAssignment::droneId)
                            .distinct()
                            .toList();

                    log.debug("Loading {} drone(s) for mission: {}", droneIds.size(), mission.id());

                    // Cargar TODOS los drones en batch (previene N+1 queries)
                    return loadDronesInBatch(droneIds)
                            .map(dronesMap -> {
                                log.debug("Loaded {} drone(s) for mission: {}",
                                        dronesMap.size(), mission.id());

                                return MissionResponseMapper.toResponse(
                                        mission,
                                        assignments,
                                        dronesMap
                                );
                            });
                });
    }

    /**
     * Carga múltiples drones en batch para evitar N+1 queries
     *
     * OPTIMIZACIÓN: Usa CompletableFuture.allOf para cargar en paralelo
     *
     * @param droneIds Lista de IDs de drones a cargar
     * @return Mono con Map de droneId -> Drone
     */
    private Mono<Map<String, Drone>> loadDronesInBatch(List<String> droneIds) {
        // Crear futures para cargar cada dron en paralelo
        List<CompletableFuture<java.util.Optional<Drone>>> droneFutures = droneIds.stream()
                .map(droneRepository::findById)
                .toList();

        // Esperar a que todos se carguen
        return Mono.fromFuture(
                CompletableFuture.allOf(
                        droneFutures.toArray(new CompletableFuture[0])
                ).thenApply(v -> {
                    // Convertir resultados a Map para lookup O(1)
                    return droneFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(java.util.Optional::isPresent)
                            .map(java.util.Optional::get)
                            .collect(Collectors.toMap(
                                    Drone::id,
                                    drone -> drone
                            ));
                })
        ).onErrorResume(error -> {
            log.error("❌ Error loading drones in batch", error);
            // En caso de error, retornar Map vacío para degradar gracefully
            return Mono.just(Map.of());
        });
    }

}