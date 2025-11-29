package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.HealthResponse;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.ports.in.MissionQueryUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionAssignmentRepository;
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

/**
 * Controller REST para consultas de misiones de drones
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission + DroneMissionAssignment separados
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
 * - Carga asignaciones de drones para cada misión
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

    /**
     * Lista todas las misiones con sus drones asignados
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
                .flatMap(mission ->
                        // Para cada misión, cargar sus asignaciones de drones
                        Mono.fromFuture(assignmentRepository.findByMissionId(mission.id()))
                                .map(assignments ->
                                        MissionResponseMapper.toResponse.apply(mission, assignments)
                                )
                )
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
     * Busca una misión por ID con sus drones asignados
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

                    // Cargar asignaciones de drones para esta misión
                    return Mono.fromFuture(
                                    assignmentRepository.findByMissionId(mission.id())
                            )
                            .map(assignments ->
                                    MissionResponseMapper.toResponse.apply(mission, assignments)
                            )
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
                .flatMap(mission ->
                        Mono.fromFuture(assignmentRepository.findByMissionId(mission.id()))
                                .map(assignments ->
                                        MissionResponseMapper.toResponse.apply(mission, assignments)
                                )
                )
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
                .flatMap(mission ->
                        Mono.fromFuture(assignmentRepository.findByMissionId(mission.id()))
                                .map(assignments ->
                                        MissionResponseMapper.toResponse.apply(mission, assignments)
                                )
                )
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

}