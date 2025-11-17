package co.cetad.umas.operation.infrastructure.web.controller;

import co.cetad.umas.operation.domain.model.dto.CreateOperationRequest;
import co.cetad.umas.operation.domain.model.dto.OperationResponse;
import co.cetad.umas.operation.domain.model.vo.DroneOperation;
import co.cetad.umas.operation.domain.ports.in.CreateDroneOperationUseCase;
import co.cetad.umas.operation.infrastructure.web.mapper.OperationResponseMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller REST para operaciones de drones
 */
@Slf4j
@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class DroneOperationController {

    private final CreateDroneOperationUseCase createOperationUseCase;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<OperationResponse>> createOperation(
            @Valid @RequestBody CreateOperationRequest request
    ) {
        log.info("POST /api/operations - Creating operation for drone: {}", request.droneId());

        DroneOperation operation = DroneOperation.create(
                request.droneId(),
                request.routeId(),
                request.startDate()
        );

        return Mono.fromFuture(createOperationUseCase.createOperation(operation))
                .map(OperationResponseMapper.toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSuccess(response ->
                        log.info("✅ Operation created successfully: {}",
                                response.getBody() != null ? response.getBody().id() : "unknown"))
                .doOnError(error ->
                        log.error("❌ Error creating operation for drone: {}",
                                request.droneId(), error))
                .onErrorResume(IllegalArgumentException.class, error ->
                        Mono.just(ResponseEntity.badRequest().body(null)))
                .onErrorResume(Exception.class, error ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)));
    }

}