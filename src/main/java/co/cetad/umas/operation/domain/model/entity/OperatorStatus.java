package co.cetad.umas.operation.domain.model.entity;

import lombok.Getter;

@Getter
public enum OperatorStatus {

    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    SUSPENDED("Suspendido");

    private final String status;

    OperatorStatus(String status) {
        this.status = status;
    }

}