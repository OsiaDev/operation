package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.CreateMissionRequest;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.DroneMission;
import co.cetad.umas.operation.domain.ports.in.CreateDroneMissionUseCase;
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
 * - POST /api/missions - Crear nueva misión manual
 */
@Slf4j
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class DroneMissionController {

    private final CreateDroneMissionUseCase createMissionUseCase;

    /**
     * Crea una nueva misión manual
     *
     * POST /api/missions
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
        log.info("POST /api/missions - Creating mission for drone: {} by commander: {}",
                request.droneId(), request.commanderName());

        // Convertir los String UUIDs del request a UUIDs
        DroneMission mission = DroneMission.createManual(
                request.name(),
                request.getDroneIdAsUUID(),
                request.getRouteIdAsUUID(),
                request.getOperatorIdAsUUID(),
                request.startDate()
        );

        return Mono.fromFuture(createMissionUseCase.createMission(mission, request.commanderName()))
                .map(MissionResponseMapper.toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSuccess(response ->
                        log.info("✅ Mission created successfully: {}",
                                response.getBody() != null ? response.getBody().id() : "unknown"))
                .doOnError(error ->
                        log.error("❌ Error creating mission for drone: {}",
                                request.droneId(), error))
                .onErrorResume(IllegalArgumentException.class, error -> {
                    log.warn("Invalid request: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(null));
                })
                .onErrorResume(Exception.class, error -> {
                    log.error("Internal error creating mission", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
                });
    }

}