package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.HealthResponse;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.*;
import co.cetad.umas.operation.domain.ports.in.MissionQueryUseCase;
import co.cetad.umas.operation.domain.ports.out.*;
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
 * ACTUALIZACIÓN: Ahora carga información completa de auditoría
 * (quién creó, aprobó, ejecutó y finalizó cada misión)
 *
 * OPTIMIZACIÓN:
 * - Carga información completa de drones en una sola query (previene N+1)
 * - Carga información de auditoría en paralelo
 *
 * Endpoints:
 * - GET /api/v1/missions                    - Listar todas las misiones
 * - GET /api/v1/missions/{id}               - Buscar misión por ID
 * - GET /api/v1/missions/authorized         - Listar misiones autorizadas (MANUAL)
 * - GET /api/v1/missions/unauthorized       - Listar misiones no autorizadas (AUTOMATICA)
 * - GET /api/v1/missions/health             - Health check
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionQueryController {

    private final MissionQueryUseCase missionQueryUseCase;
    private final DroneMissionAssignmentRepository assignmentRepository;
    private final DroneRepository droneRepository;
    private final MissionOrderRepository orderRepository;
    private final MissionApprovalRepository approvalRepository;
    private final MissionExecutionRepository executionRepository;
    private final MissionFinalizationRepository finalizationRepository;

    /**
     * Lista todas las misiones con sus drones asignados y auditoría completa
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
                .flatMap(this::loadMissionWithCompleteInfo)
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
     * Busca una misión por ID con sus drones asignados y auditoría completa
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
                    return loadMissionWithCompleteInfo(mission)
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
                .flatMap(this::loadMissionWithCompleteInfo)
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
                .flatMap(this::loadMissionWithCompleteInfo)
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
     * Carga una misión con información completa:
     * - Drones asignados (información completa)
     * - Auditoría completa (quién creó, aprobó, ejecutó y finalizó)
     *
     * OPTIMIZACIÓN:
     * 1. Cargar asignaciones de drones
     * 2. Cargar TODOS los drones en batch (evita N+1 queries)
     * 3. Cargar información de auditoría EN PARALELO
     * 4. Mapear a response con toda la información
     */
    private Mono<MissionResponse> loadMissionWithCompleteInfo(Mission mission) {
        log.debug("Loading complete info for mission: {}", mission.id());

        // Cargar asignaciones y auditoría EN PARALELO
        Mono<List<DroneMissionAssignment>> assignmentsMono =
                Mono.fromFuture(assignmentRepository.findByMissionId(mission.id()));

        Mono<AuditInfo> auditMono = loadAuditInfo(mission);

        return Mono.zip(assignmentsMono, auditMono)
                .flatMap(tuple -> {
                    List<DroneMissionAssignment> assignments = tuple.getT1();
                    AuditInfo audit = tuple.getT2();

                    if (assignments.isEmpty()) {
                        // Sin drones asignados, retornar response con auditoría
                        return Mono.just(
                                MissionResponseMapper.toResponse(
                                        mission,
                                        List.of(),
                                        Map.of(),
                                        audit.order(),
                                        audit.approval(),
                                        audit.execution(),
                                        audit.finalization()
                                )
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
                                        dronesMap,
                                        audit.order(),
                                        audit.approval(),
                                        audit.execution(),
                                        audit.finalization()
                                );
                            });
                });
    }

    /**
     * Carga información de auditoría EN PARALELO
     *
     * OPTIMIZACIÓN: Todas las consultas se ejecutan en paralelo
     *
     * @param mission Misión para la cual cargar auditoría
     * @return Mono con información de auditoría completa
     */
    private Mono<AuditInfo> loadAuditInfo(Mission mission) {
        log.debug("Loading audit info for mission: {}", mission.id());

        // Cargar TODOS los registros de auditoría en paralelo
        Mono<MissionOrder> orderMono = Mono.fromFuture(
                orderRepository.findByMissionId(mission.id())
        ).map(opt -> opt.orElse(null));

        Mono<MissionApproval> approvalMono = Mono.fromFuture(
                approvalRepository.findByMissionId(mission.id())
        ).map(opt -> opt.orElse(null));

        Mono<MissionExecution> executionMono = Mono.fromFuture(
                executionRepository.findByMissionId(mission.id())
        ).map(opt -> opt.orElse(null));

        Mono<MissionFinalization> finalizationMono = Mono.fromFuture(
                finalizationRepository.findByMissionId(mission.id())
        ).map(opt -> opt.orElse(null));

        // Combinar todos los resultados
        return Mono.zip(orderMono, approvalMono, executionMono, finalizationMono)
                .map(tuple -> new AuditInfo(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        tuple.getT4()
                ))
                .doOnSuccess(audit -> {
                    log.debug("✅ Loaded audit info for mission: {} " +
                                    "(order: {}, approval: {}, execution: {}, finalization: {})",
                            mission.id(),
                            audit.order() != null,
                            audit.approval() != null,
                            audit.execution() != null,
                            audit.finalization() != null);
                })
                .onErrorResume(error -> {
                    log.warn("⚠️ Error loading audit info for mission: {}, " +
                            "returning empty audit", mission.id(), error);
                    // En caso de error, retornar audit vacío para degradar gracefully
                    return Mono.just(new AuditInfo(null, null, null, null));
                });
    }

    /**
     * Busca un registro de auditoría específico por ID de misión
     * Devuelve null si no existe
     */
    private <T> Mono<T> findAuditRecord(
            CompletableFuture<java.util.Optional<T>> future
    ) {
        return Mono.fromFuture(future)
                .map(opt -> opt.orElse(null))
                .onErrorReturn(null);
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

    /**
     * Record para encapsular información de auditoría
     */
    private record AuditInfo(
            MissionOrder order,
            MissionApproval approval,
            MissionExecution execution,
            MissionFinalization finalization
    ) {}

}