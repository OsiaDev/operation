package co.cetad.umas.operation.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "mission_approval")
public class MissionApprovalEntity implements Serializable, Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "mission_id")
    private UUID missionId;

    @Column(name = "commander_name")
    private String commanderName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

}