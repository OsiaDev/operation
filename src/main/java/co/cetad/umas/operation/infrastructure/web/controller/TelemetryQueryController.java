package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.HealthResponse;
import co.cetad.umas.operation.domain.model.dto.TelemetryResponse;
import co.cetad.umas.operation.domain.ports.in.TelemetryQueryUseCase;
import co.cetad.umas.operation.infrastructure.web.mapper.TelemetryResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Controller REST para consultas de telemetría de drones
 *
 * Endpoints:
 * - GET /api/telemetry/{id}                              - Buscar por ID
 * - GET /api/telemetry/vehicle/{vehicleId}/latest        - Última telemetría
 * - GET /api/telemetry/vehicle/{vehicleId}/range         - Por rango de fechas
 * - GET /api/telemetry/vehicle/{vehicleId}/recent        - Telemetría reciente
 *
 * Usa WebFlux para respuestas reactivas
 */
@Slf4j
@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
public class TelemetryQueryController {

    private final TelemetryQueryUseCase telemetryQueryUseCase;

    /**
     * Busca telemetría por ID
     *
     * GET /api/telemetry/{id}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<TelemetryResponse>> findById(
            @PathVariable String id
    ) {
        log.info("GET /api/telemetry/{}", id);

        return Mono.fromFuture(telemetryQueryUseCase.findById(id))
                .map(optional -> optional
                        .map(TelemetryResponseMapper.toResponse)
                        .map(ResponseEntity::ok)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Telemetry not found with id: " + id
                        ))
                )
                .doOnSuccess(response -> log.info("Found telemetry with id: {}", id))
                .doOnError(error -> log.error("Error finding telemetry with id: {}", id, error));
    }

    /**
     * Busca la última telemetría de un vehículo
     *
     * GET /api/telemetry/vehicle/{vehicleId}/latest
     */
    @GetMapping(value = "/vehicle/{vehicleId}/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<TelemetryResponse>> findLatestByVehicleId(
            @PathVariable String vehicleId
    ) {
        log.info("GET /api/telemetry/vehicle/{}/latest", vehicleId);

        return Mono.fromFuture(telemetryQueryUseCase.findLatestByVehicleId(vehicleId))
                .map(optional -> optional
                        .map(TelemetryResponseMapper.toResponse)
                        .map(ResponseEntity::ok)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No telemetry found for vehicle: " + vehicleId
                        ))
                )
                .doOnSuccess(response -> log.info("Found latest telemetry for vehicle: {}", vehicleId))
                .doOnError(error -> log.error("Error finding latest telemetry for vehicle: {}", vehicleId, error));
    }

    /**
     * Busca telemetría de un vehículo en un rango de fechas
     *
     * GET /api/telemetry/vehicle/{vehicleId}/range?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
     *
     * @param vehicleId  ID del vehículo
     * @param startDate  Fecha inicio en formato ISO: yyyy-MM-dd'T'HH:mm:ss
     * @param endDate    Fecha fin en formato ISO: yyyy-MM-dd'T'HH:mm:ss
     */
    @GetMapping(value = "/vehicle/{vehicleId}/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<TelemetryResponse> findByVehicleIdAndDateRange(
            @PathVariable String vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("GET /api/telemetry/vehicle/{}/range?startDate={}&endDate={}",
                vehicleId, startDate, endDate);

        return Mono.fromFuture(
                        telemetryQueryUseCase.findByVehicleIdAndDateRange(vehicleId, startDate, endDate)
                )
                .flatMapMany(Flux::fromIterable)
                .map(TelemetryResponseMapper.toResponse)
                .doOnComplete(() -> log.info("Completed query for vehicle: {} in date range", vehicleId))
                .doOnError(error -> {
                    log.error("Error querying telemetry for vehicle: {} in date range", vehicleId, error);
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Error querying telemetry: " + error.getMessage(),
                            error
                    );
                });
    }

    /**
     * Busca telemetría reciente de un vehículo (últimas N entradas)
     *
     * GET /api/telemetry/vehicle/{vehicleId}/recent?limit=10
     *
     * @param vehicleId  ID del vehículo
     * @param limit      Número de registros a retornar (por defecto 10, máximo 1000)
     */
    @GetMapping(value = "/vehicle/{vehicleId}/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<TelemetryResponse> findRecentByVehicleId(
            @PathVariable String vehicleId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("GET /api/telemetry/vehicle/{}/recent?limit={}", vehicleId, limit);

        return Mono.fromFuture(
                        telemetryQueryUseCase.findRecentByVehicleId(vehicleId, limit)
                )
                .flatMapMany(Flux::fromIterable)
                .map(TelemetryResponseMapper.toResponse)
                .doOnComplete(() -> log.info("Completed recent query for vehicle: {} with limit: {}",
                        vehicleId, limit))
                .doOnError(error -> {
                    log.error("Error querying recent telemetry for vehicle: {}", vehicleId, error);
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Error querying telemetry: " + error.getMessage(),
                            error
                    );
                });
    }

    /**
     * Health check del controller
     *
     * GET /api/telemetry/health
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<HealthResponse>> health() {
        return Mono.just(ResponseEntity.ok(
                new HealthResponse("UP", "Telemetry Query Controller is running")
        ));
    }

}