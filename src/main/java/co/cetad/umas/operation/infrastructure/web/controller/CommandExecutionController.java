package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.application.service.CommandExecutionService;
import co.cetad.umas.operation.domain.model.dto.CommandRequest;
import co.cetad.umas.operation.domain.model.dto.HealthResponse;
import co.cetad.umas.operation.domain.ports.in.ExecuteCommandUseCase;
import co.cetad.umas.operation.infrastructure.ugcs.config.CommandUgcsProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller REST para comandos de drones UGCS
 *
 * REFACTORIZACIÓN: Ahora los comandos se envían a TODOS los drones asignados a la misión
 *
 * Endpoints:
 * - POST /api/v1/commands/{missionId}/return-to-home   - Retornar a casa
 * - POST /api/v1/commands/{missionId}/takeoff          - Despegar
 * - POST /api/v1/commands/{missionId}/land             - Aterrizar
 * - POST /api/v1/commands/{missionId}/emergency-land   - Aterrizaje de emergencia
 * - POST /api/v1/commands/{missionId}/pause            - Pausar ruta
 * - POST /api/v1/commands/{missionId}/resume           - Reanudar ruta
 *
 * Todos los endpoints reciben el ID de la misión como path param
 * y opcionalmente el nombre del comandante en el body
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/commands")
@RequiredArgsConstructor
public class CommandExecutionController {

    private final ExecuteCommandUseCase executeCommandUseCase;
    private final CommandUgcsProperties commandProperties;

    /**
     * Comando: Return to Home
     * Envía al dron de vuelta a su posición de origen
     *
     * POST /api/v1/commands/{missionId}/return-to-home
     */
    @PostMapping(
            value = "/{missionId}/return-to-home",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<HealthResponse>> returnToHome(
            @PathVariable String missionId,
            @Valid @RequestBody(required = false) CommandRequest request
    ) {
        String commander = extractCommander(request);
        log.info("POST /api/v1/commands/{}/return-to-home - Commander: {}",
                missionId, commander);

        return executeCommand(missionId, commandProperties.getHome(), "Return to Home");
    }

    /**
     * Comando: Takeoff
     * Despegar el dron
     *
     * POST /api/v1/commands/{missionId}/takeoff
     */
    @PostMapping(
            value = "/{missionId}/takeoff",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<HealthResponse>> takeoff(
            @PathVariable String missionId,
            @Valid @RequestBody(required = false) CommandRequest request
    ) {
        String commander = extractCommander(request);
        log.info("POST /api/v1/commands/{}/takeoff - Commander: {}",
                missionId, commander);

        return executeCommand(missionId, commandProperties.getTakeOff(), "Takeoff");
    }

    /**
     * Comando: Land
     * Aterrizar el dron normalmente
     *
     * POST /api/v1/commands/{missionId}/land
     */
    @PostMapping(
            value = "/{missionId}/land",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<HealthResponse>> land(
            @PathVariable String missionId,
            @Valid @RequestBody(required = false) CommandRequest request
    ) {
        String commander = extractCommander(request);
        log.info("POST /api/v1/commands/{}/land - Commander: {}",
                missionId, commander);

        return executeCommand(missionId, commandProperties.getLand(), "Land");
    }

    /**
     * Comando: Emergency Land
     * Aterrizaje de emergencia
     *
     * POST /api/v1/commands/{missionId}/emergency-land
     */
    @PostMapping(
            value = "/{missionId}/emergency-land",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<HealthResponse>> emergencyLand(
            @PathVariable String missionId,
            @Valid @RequestBody(required = false) CommandRequest request
    ) {
        String commander = extractCommander(request);
        log.info("POST /api/v1/commands/{}/emergency-land - Commander: {}",
                missionId, commander);

        return executeCommand(missionId, commandProperties.getEmergencyLand(), "Emergency Land");
    }

    /**
     * Comando: Pause Route
     * Pausar la ruta actual
     *
     * POST /api/v1/commands/{missionId}/pause
     */
    @PostMapping(
            value = "/{missionId}/pause",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<HealthResponse>> pause(
            @PathVariable String missionId,
            @Valid @RequestBody(required = false) CommandRequest request
    ) {
        String commander = extractCommander(request);
        log.info("POST /api/v1/commands/{}/pause - Commander: {}",
                missionId, commander);

        return executeCommand(missionId, commandProperties.getPause(), "Pause Route");
    }

    /**
     * Comando: Resume Route
     * Reanudar la ruta pausada
     *
     * POST /api/v1/commands/{missionId}/resume
     */
    @PostMapping(
            value = "/{missionId}/resume",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<HealthResponse>> resume(
            @PathVariable String missionId,
            @Valid @RequestBody(required = false) CommandRequest request
    ) {
        String commander = extractCommander(request);
        log.info("POST /api/v1/commands/{}/resume - Commander: {}",
                missionId, commander);

        return executeCommand(missionId, commandProperties.getResume(), "Resume Route");
    }

    /**
     * Health check del controller
     *
     * GET /api/v1/commands/health
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<HealthResponse>> health() {
        return Mono.just(ResponseEntity.ok(
                new HealthResponse("UP", "Command Execution Controller is running")
        ));
    }

    /**
     * Método auxiliar para ejecutar comandos
     */
    private Mono<ResponseEntity<HealthResponse>> executeCommand(
            String missionId,
            String commandCode,
            String commandName
    ) {
        return Mono.fromFuture(executeCommandUseCase.executeCommand(missionId, commandCode))
                .map(result -> ResponseEntity.ok(
                        new HealthResponse(
                                "SUCCESS",
                                String.format("%s command sent successfully for mission: %s",
                                        commandName, missionId)
                        )
                ))
                .doOnSuccess(response ->
                        log.info("✅ {} command executed successfully for mission: {}",
                                commandName, missionId))
                .doOnError(error ->
                        log.error("❌ Error executing {} command for mission: {}",
                                commandName, missionId, error))
                .onErrorResume(error -> handleCommandError(error, missionId, commandName));
    }

    /**
     * Extrae el nombre del comandante del request (si existe)
     */
    private String extractCommander(CommandRequest request) {
        return request != null && request.commanderName() != null
                ? request.commanderName()
                : "System";
    }

    /**
     * Maneja errores de ejecución de comandos con pattern matching
     */
    private Mono<ResponseEntity<HealthResponse>> handleCommandError(
            Throwable error,
            String missionId,
            String commandName
    ) {
        return switch (error) {
            case IllegalArgumentException e -> {
                log.warn("⚠️ Invalid request for mission {}: {}", missionId, e.getMessage());
                yield Mono.just(ResponseEntity
                        .badRequest()
                        .body(new HealthResponse("ERROR", e.getMessage())));
            }
            case CommandExecutionService.MissionNotFoundException e -> {
                log.warn("⚠️ Mission not found: {}", missionId);
                yield Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new HealthResponse("ERROR", "Mission not found")));
            }
            case CommandExecutionService.NoDronesAssignedException e -> {
                log.warn("⚠️ No drones assigned to mission: {}", missionId);
                yield Mono.just(ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new HealthResponse("ERROR", "No drones assigned to this mission")));
            }
            case CommandExecutionService.DroneNotFoundException e -> {
                log.warn("⚠️ Drone not found for mission: {}", missionId);
                yield Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new HealthResponse("ERROR", "Drone not found")));
            }
            case CommandExecutionService.CommandPublishException e -> {
                log.error("❌ Error publishing command for mission: {}", missionId, e);
                yield Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new HealthResponse("ERROR", "Failed to publish command")));
            }
            default -> {
                log.error("❌ Unexpected error executing {} command for mission: {}",
                        commandName, missionId, error);
                yield Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new HealthResponse("ERROR", "Internal server error")));
            }
        };
    }

}