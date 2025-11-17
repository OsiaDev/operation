package co.cetad.umas.operation.infrastructure.web.mapper;


import co.cetad.umas.operation.domain.model.vo.DroneOperation;

import java.util.function.Function;

/**
 * Mapper funcional de dominio a DTO response
 */
public final class OperationResponseMapper {

    private OperationResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final Function<DroneOperation, OperationResponse> toResponse = operation ->
            OperationResponse.from(
                    operation.id(),
                    operation.droneId(),
                    operation.routeId(),
                    operation.startDate(),
                    operation.createdAt(),
                    operation.updatedAt(),
                    operation.hasRoute(),
                    operation.isScheduledForFuture()
            );

}