package co.cetad.umas.operation.infrastructure.web.mapper;

import co.cetad.umas.operation.domain.model.dto.MissionResponse;
import co.cetad.umas.operation.domain.model.vo.DroneMission;

import java.util.function.Function;

/**
 * Mapper funcional de dominio a DTO response
 */
public final class MissionResponseMapper {

    private MissionResponseMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final Function<DroneMission, MissionResponse> toResponse = mission ->
            MissionResponse.from(
                    mission.id(),
                    mission.name(),
                    mission.droneId(),
                    mission.routeId(),
                    mission.operatorId(),
                    mission.missionType(),
                    mission.state(),
                    mission.startDate(),
                    mission.createdAt(),
                    mission.updatedAt(),
                    mission.hasRoute(),
                    mission.hasName(),
                    mission.isScheduledForFuture(),
                    mission.isManual(),
                    mission.isPendingApproval()
            );

}