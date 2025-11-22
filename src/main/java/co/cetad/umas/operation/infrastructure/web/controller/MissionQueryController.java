package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.HealthResponse;
import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.ports.in.MissionQueryUseCase;
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
 * Endpoints:
 * - GET /api/v1/missions           - Listar todas las misiones
 * - GET /api/v1/missions/{id}      - Buscar misión por ID
 *
 * Usa WebFlux para respuestas reactivas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
public class MissionQueryController {

    private final MissionQueryUseCase missionQueryUseCase;

    /**
     * Lista todas las misiones
     *
     * GET /api/v1/missions
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<MissionResponse> findAll() {
        log.info("GET /api/v1/missions - Listing all missions");

        return Mono.fromFuture(missionQueryUseCase.findAll())
                .flatMapMany(Flux::fromIterable)
                .map(MissionResponseMapper.toResponse)
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
     * Busca una misión por ID
     *
     * GET /api/v1/missions/{id}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MissionResponse>> findById(
            @PathVariable String id
    ) {
        log.info("GET /api/v1/missions/{}", id);

        return Mono.fromFuture(missionQueryUseCase.findById(id))
                .map(optional -> optional
                        .map(MissionResponseMapper.toResponse)
                        .map(ResponseEntity::ok)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Mission not found with id: " + id
                        ))
                )
                .doOnSuccess(response -> log.info("✅ Found mission with id: {}", id))
                .doOnError(error -> log.error("❌ Error finding mission with id: {}", id, error));
    }

    /**
     * Health check del controller
     *
     * GET /api/v1/missions/health
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<HealthResponse>> health() {
        return Mono.just(ResponseEntity.ok(
                new HealthResponse("UP", "Mission Query Controller is running")
        ));
    }

}