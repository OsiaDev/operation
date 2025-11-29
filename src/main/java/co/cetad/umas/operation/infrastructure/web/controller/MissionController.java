package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.application.service.ApproveMissionService;
import co.cetad.umas.operation.application.service.CreateMissionService;
import co.cetad.umas.operation.application.service.ExecuteMissionService;
import co.cetad.umas.operation.domain.model.dto.ApproveMissionRequest;
import co.cetad.umas.operation.domain.model.dto.CreateMissionRequest;
import co.cetad.umas.operation.domain.model.dto.ExecuteMissionRequest;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.DroneMissionAssignment;
import co.cetad.umas.operation.domain.model.vo.Mission;
import co.cetad.umas.operation.domain.ports.in.ApproveMissionUseCase;
import co.cetad.umas.operation.domain.ports.in.CreateMissionUseCase;
import co.cetad.umas.operation.domain.ports.in.ExecuteMissionUseCase;
import co.cetad.umas.operation.domain.ports.out.DroneMissionAssignmentRepository;
import co.cetad.umas.operation.infrastructure.web.mapper.MissionResponseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller REST para misiones con múltiples drones
 *
 * REFACTORIZACIÓN: Ahora trabaja con Mission + DroneMissionAssignment
 *
 * Endpoints:
 * - POST /api/v1/missions                  - Crear misión con drones asignados
 * - POST /api/v1/missions/{id}/approve     - Aprobar misión
 * - POST /api/v1/missions/{id}/execute     - Ejecutar misión (todos los drones)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionController {

    private final CreateMissionUseCase createMissionUseCase;
    private final ApproveMissionUseCase approveMissionUseCase;
    private final ExecuteMissionUseCase executeMissionUseCase;
    private final DroneMissionAssignmentRepository assignmentRepository;

    /**
     * Crea una nueva misión con múltiples drones asignados
     *
     * POST /api/v1/missions
     *
     * Body:
     * {
     *   "name": "Patrullaje Zona Norte",
     *   "operatorId": "550e8400-e29b-41d4-a716-446655440000",
     *   "commanderName": "Juan Pérez",
     *   "estimatedDate": "2025-11-30T10:00:00",
     *   "droneAssignments": [
     *     {
     *       "droneId": "650e8400-e29b-41d4-a716-446655440001",
     *       "routeId": "750e8400-e29b-41d4-a716-446655440001"
     *     },
     *     {
     *       "droneId": "650e8400-e29b-41d4-a716-446655440002",
     *       "routeId": "750e8400-e29b-41d4-a716-446655440002"
     *     }
     *   ]
     * }
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<MissionResponse>> createMission(
            @Valid @RequestBody CreateMissionRequest request
    ) {
        log.info("POST /api/v1/missions - Creating mission with {} drone(s) by commander: {}",
                request.droneAssignments().size(), request.commanderName());

        Mission mission = buildMission(request);
        List<DroneMissionAssignment> assignments = buildDroneAssignments(mission.id(), request);

        return Mono.fromFuture(
                        createMissionUseCase.createMission(
                                mission,
                                request.commanderName(),
                                assignments
                        )
                )
                .flatMap(this::loadAssignmentsAndMap)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSuccess(response ->
                        log.info("✅ Mission created successfully: {} with {} drone(s)",
                                extractResponseId(response), request.droneAssignments().size()))
                .doOnError(error ->
                        log.error("❌ Error creating mission", error))
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
                .flatMap(this::loadAssignmentsAndMap)
                .map(ResponseEntity::ok)
                .doOnSuccess(response ->
                        log.info("✅ Mission approved successfully: {} by commander: {}",
                                extractResponseId(response), request.commanderName()))
                .doOnError(error ->
                        log.error("❌ Error approving mission: {}", id, error))
                .onErrorResume(error -> handleApproveError(error, id));
    }

    /**
     * Ejecuta una misión aprobada (envía comandos a TODOS los drones asignados)
     *
     * POST /api/v1/missions/{id}/execute
     *
     * Body:
     * {
     *   "commanderName": "Carlos Rodríguez"
     * }
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
                .flatMap(this::loadAssignmentsAndMap)
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
     */
    private Mission buildMission(CreateMissionRequest request) {
        return Mission.createManual(
                request.name(),
                request.operatorId(),  // String UUID
                request.estimatedDate()
        );
    }

    /**
     * Construye las asignaciones de drones desde el request
     */
    private List<DroneMissionAssignment> buildDroneAssignments(
            String missionId,
            CreateMissionRequest request
    ) {
        return request.droneAssignments().stream()
                .map(droneReq -> DroneMissionAssignment.create(
                        missionId,
                        droneReq.droneId(),
                        droneReq.routeId()
                ))
                .toList();
    }

    /**
     * Carga las asignaciones de drones y mapea a response
     */
    private Mono<MissionResponse> loadAssignmentsAndMap(Mission mission) {
        return Mono.fromFuture(
                        assignmentRepository.findByMissionId(mission.id())
                )
                .map(assignments -> MissionResponseMapper.toResponse.apply(mission, assignments));
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
            case CreateMissionService.MissionCreationException e -> {
                log.error("❌ Mission creation failed", e);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
            case CreateMissionService.DroneAssignmentException e -> {
                log.error("❌ Drone assignment failed", e);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
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
            case ApproveMissionService.MissionNotFoundException e -> {
                log.warn("⚠️ Mission not found: {}", missionId);
                yield Mono.just(ResponseEntity.notFound().build());
            }
            case ApproveMissionService.InvalidMissionStateException e -> {
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
            case ExecuteMissionService.MissionNotFoundException e -> {
                log.warn("⚠️ Mission not found: {}", missionId);
                yield Mono.just(ResponseEntity.notFound().build());
            }
            case ExecuteMissionService.NoDronesAssignedException e -> {
                log.warn("⚠️ No drones assigned to mission: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            case ExecuteMissionService.DroneNotFoundException e -> {
                log.warn("⚠️ Drone not found for mission: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            }
            case ExecuteMissionService.RouteNotFoundException e -> {
                log.warn("⚠️ Route not found for mission: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            }
            case ExecuteMissionService.InvalidMissionStateException e -> {
                log.warn("⚠️ Invalid mission state for execution: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            case ExecuteMissionService.MissionScheduledForFutureException e -> {
                log.warn("⚠️ Mission scheduled for future: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            case ExecuteMissionService.InvalidRouteException e -> {
                log.warn("⚠️ Invalid route for mission: {}", missionId);
                yield Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build());
            }
            case ExecuteMissionService.RouteParsingException e -> {
                log.error("❌ Error parsing route GeoJSON for mission: {}", missionId, e);
                yield Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build());
            }
            case ExecuteMissionService.CommandPublishException e -> {
                log.error("❌ Error publishing execution command for mission: {}", missionId, e);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
            default -> {
                log.error("❌ Unexpected error executing mission: {}", missionId, error);
                yield Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            }
        };
    }

}