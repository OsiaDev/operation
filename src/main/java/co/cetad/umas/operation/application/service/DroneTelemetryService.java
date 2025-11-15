package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.model.vo.GeoLocation;
import co.cetad.umas.operation.domain.model.vo.TelemetryEvent;
import co.cetad.umas.operation.domain.model.vo.TelemetryMetrics;
import co.cetad.umas.operation.domain.ports.in.EventProcessor;
import co.cetad.umas.operation.domain.ports.out.DroneTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio simplificado que solo almacena telemetría
 *
 * Responsabilidad única:
 * 1. Transformar evento a modelo de dominio
 * 2. Persistir en base de datos
 *
 * Sin validaciones de negocio ni generación de alertas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneTelemetryService implements EventProcessor<DroneTelemetry, TelemetryEvent> {

    private final DroneTelemetryRepository telemetryRepository;

    /**
     * Procesa y almacena un evento de telemetría
     */
    @Override
    public CompletableFuture<DroneTelemetry> process(TelemetryEvent event) {
        log.debug("Processing telemetry for vehicle: {}", event.vehicleId());

        return CompletableFuture
                .supplyAsync(() -> transformToDomain(event))
                .thenCompose(telemetryRepository::save)
                .exceptionally(throwable -> {
                    log.error("Failed to process telemetry for vehicle: {}",
                            event.vehicleId(), throwable);
                    throw new RuntimeException(
                            "Telemetry processing failed for vehicle: " + event.vehicleId(),
                            throwable
                    );
                });
    }

    /**
     * Transforma el evento a modelo de dominio
     */
    private DroneTelemetry transformToDomain(TelemetryEvent event) {
        GeoLocation location = event.toGeoLocation();
        TelemetryMetrics metrics = event.toTelemetryMetrics();

        return DroneTelemetry.create(
                event.vehicleId(),
                location,
                metrics
        );
    }

}