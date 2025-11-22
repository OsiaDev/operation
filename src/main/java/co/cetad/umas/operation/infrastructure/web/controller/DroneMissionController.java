package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.ApproveMissionRequest;
import co.cetad.umas.operation.domain.model.dto.CreateMissionRequest;
import co.cetad.umas.operation.domain.model.dto.ExecuteMissionRequest;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.ports.in.ApproveMissionUseCase;
import co.cetad.umas.operation.domain.ports.in.CreateDroneMissionUseCase;
import co.cetad.umas.operation.domain.ports.in.ExecuteMissionUseCase;
import co.cetad.umas.operation.infrastructure.web.mapper.MissionResponseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller REST para misiones de drones
 *
 * Endpoints:
 * - POST /api/v1/missions - Crear nueva misión manual
 * - POST /api/v1/missions/{id}/approve - Aprobar misión
 * - POST /api/v1/missions/{id}/execute - Ejecutar misión
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class DroneMissionController {

    private final CreateDroneMissionUseCase createMissionUseCase;
    private final ApproveMissionUseCase approveMissionUseCase;
    private final ExecuteMissionUseCase executeMissionUseCase;

    /**
     * Crea una nueva misión manual
     *
     * POST /api/v1/missions
     *
     * Body:
     * {
     *   "name": "Misión de Reconocimiento",
     *   "droneId": "550e8400-e29b-41d4-a716-446655440000",
     *   "routeId": "650e8400-e29b-41d4-a716-446655440000",
     *   "operatorId": "750e8400-e29b-41d4-a716-446655440000",
     *   "commanderName": "Juan Pérez",
     *   "startDate": "2025-11-22T10:00:00"
     * }
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<MissionResponse>> createMission(
            @Valid @RequestBody CreateMissionRequest request
    ) {
        log.info("POST /api/v1/missions - Creating mission for drone: {} by commander: {}",
                request.droneId(), request.commanderName());

        return Mono.fromFuture(
                        createMissionUseCase.createMission(
                                buildMission(request),
                                request.commanderName()
                        )
                )
                .map(MissionResponseMapper.toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSuccess(response ->
                        log.info("✅ Mission created successfully: {}",
                                extractResponseId(response)))
                .doOnError(error ->
                        log.error("❌ Error creating mission for drone: {}",
                                request.droneId(), error))
                .onErrorResume(this::handleCreateError);
    }

    /**
     * Aprueba una misión existente
     *
     * POST /api/v1/missions/{id}/approve
     *
     * Body:
     * {
     *   "commanderName": "María García"
     * }
     *
     * @param id ID de la misión a aprobar (UUID como String)
     * @param request Request con el nombre del comandante que aprueba
     * @return Misión aprobada con estado APROBADA
     */
    @PostMapping(
            value = "/{id}/approve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<MissionResponse>> approveMission(
            @PathVariable String id,
            @Valid @RequestBody ApproveMissionRequest request
    ) {
        log.info("POST /api/v1/missions/{}/approve - Approving mission by commander: {}",
                id, request.commanderName());

        return Mono.fromFuture(
                        approveMissionUseCase.approveMission(id, request.commanderName())
                )
                .map(MissionResponseMapper.toResponse)
                .map(ResponseEntity::ok)
                .doOnSuccess(response ->
                        log.info("✅ Mission approved successfully: {} by commander: {}",
                                extractResponseId(response), request.commanderName()))
                .doOnError(error ->
                        log.error("❌ Error approving mission: {}", id, error))
                .onErrorResume(error -> handleApproveError(error, id));
    }

    /**
     * Ejecuta una misión aprobada
     *
     * POST /api/v1/missions/{id}/execute
     *
     * Body:
     * {
     *   "commanderName": "Carlos Rodríguez"
     * }
     *
     * @param id ID de la misión a ejecutar (UUID como String)
     * @param request Request con el nombre del comandante que autoriza la ejecución
     * @return Misión en ejecución con estado EN_EJECUCION
     */
    @PostMapping(
            value = "/{id}/execute",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<MissionResponse>> executeMission(
            @PathVariable String id,
            @Valid @RequestBody ExecuteMissionRequest request
    ) {
        log.info("POST /api/v1/missions/{}/execute - Executing mission by commander: {}",
                id, request.commanderName());

        return Mono.fromFuture(
                        executeMissionUseCase.executeMission(id, request.commanderName())
                )
                .map(MissionResponseMapper.toResponse)
                .map(ResponseEntity::ok)
                .doOnSuccess(response ->
                        log.info("✅ Mission executed successfully: {} by commander: {}",
                                extractResponseId(response), request.commanderName()))
                .doOnError(error ->
                        log.error("❌ Error executing mission: {}", id, error))
                .onErrorResume(error -> handleExecuteError(error, id));
    }

    /**
     * Construye la misión desde el request
     * Los IDs ya vienen validados como UUIDs en el CreateMissionRequest
     */
    private DroneMission buildMission(CreateMissionRequest request) {
        return DroneMission.createManual(
                request.name(),
                request.droneId(),      // String UUID
                request.routeId(),      // String UUID (puede ser null)
                request.operatorId(),   // String UUID
                request.startDate()
        );
    }

    /**
     * Extrae el ID de la respuesta de forma segura
     */
    private String extractResponseId(ResponseEntity<MissionResponse> response) {
        return response.getBody() != null
                ? response.getBody().id().toString()
                : "unknown";
    }

    /**
     * Maneja errores de creación de misión con pattern matching
     */
    private Mono<ResponseEntity<MissionResponse>> handleCreateError(Throwable error) {
        return switch (error) {
            case IllegalArgumentException e -> {
                log.warn("⚠️ Invalid request: {}", e.getMessage());
                yield Mono.just(ResponseEntity.badRequest().build());
            }
            case co.cetad.umas.operation.application.service.DroneMissionService.MissionCreationException e -> {
                log.error("❌ Mission creation failed", e);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
            default -> {
                log.error("❌ Unexpected error creating mission", error);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        };
    }

    /**
     * Maneja errores de aprobación de misión con pattern matching
     */
    private Mono<ResponseEntity<MissionResponse>> handleApproveError(Throwable error, String missionId) {
        return switch (error) {
            case IllegalArgumentException e -> {
                log.warn("⚠️ Invalid request for mission {}: {}", missionId, e.getMessage());
                yield Mono.just(ResponseEntity.badRequest().build());
            }
            case co.cetad.umas.operation.application.service.ApproveMissionService.MissionNotFoundException e -> {
                log.warn("⚠️ Mission not found: {}", missionId);
                yield Mono.just(ResponseEntity.notFound().build());
            }
            case co.cetad.umas.operation.application.service.ApproveMissionService.InvalidMissionStateException e -> {
                log.warn("⚠️ Invalid mission state for approval: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            default -> {
                log.error("❌ Unexpected error approving mission: {}", missionId, error);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        };
    }

    /**
     * Maneja errores de ejecución de misión con pattern matching
     */
    private Mono<ResponseEntity<MissionResponse>> handleExecuteError(Throwable error, String missionId) {
        return switch (error) {
            case IllegalArgumentException e -> {
                log.warn("⚠️ Invalid request for mission {}: {}", missionId, e.getMessage());
                yield Mono.just(ResponseEntity.badRequest().build());
            }
            case co.cetad.umas.operation.application.service.ExecuteMissionService.MissionNotFoundException e -> {
                log.warn("⚠️ Mission not found: {}", missionId);
                yield Mono.just(ResponseEntity.notFound().build());
            }
            case co.cetad.umas.operation.application.service.ExecuteMissionService.InvalidMissionStateException e -> {
                log.warn("⚠️ Invalid mission state for execution: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            case co.cetad.umas.operation.application.service.ExecuteMissionService.MissionScheduledForFutureException e -> {
                log.warn("⚠️ Mission scheduled for future: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            default -> {
                log.error("❌ Unexpected error executing mission: {}", missionId, error);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        };
    }

}