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
import java.util.function.Function;

/**
 * Servicio de aplicación que procesa eventos de telemetría
 * Orquesta la transformación del evento a modelo de dominio y su persistencia
 * Sigue principios de clean code y programación funcional
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneTelemetryService implements EventProcessor<DroneTelemetry, TelemetryEvent> {

    private final DroneTelemetryRepository telemetryRepository;

    /**
     * Procesa un evento de telemetría de forma asíncrona
     * 1. Transforma el evento a modelo de dominio
     * 2. Valida los datos
     * 3. Persiste en base de datos
     * 4. Registra alertas si es necesario
     */
    @Override
    public CompletableFuture<DroneTelemetry> process(TelemetryEvent event) {
        log.debug("Processing telemetry event for vehicle: {}", event.vehicleId());

        return CompletableFuture
                .supplyAsync(() -> transformToDomain(event))
                .thenCompose(telemetryRepository::save)
                .thenApply(saved -> {
                    logAlertIfNeeded(event);
                    return saved;
                })
                .exceptionally(handleError(event.vehicleId()));
    }

    /**
     * Transforma el evento de telemetría a modelo de dominio
     * Usa los métodos del evento para extraer ubicación y métricas
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

    /**
     * Registra alertas si el evento las requiere
     * En una implementación completa, esto podría publicar eventos de alerta
     */
    private void logAlertIfNeeded(TelemetryEvent event) {
        if (event.requiresAlert()) {
            String alertReason = event.getAlertReason();
            log.warn("ALERT for vehicle {}: {}", event.vehicleId(), alertReason);
            // TODO: Publicar evento de alerta a Kafka o sistema de notificaciones
        }
    }

    /**
     * Manejador funcional de errores
     * Retorna una función que logea el error y lanza una excepción
     */
    private Function<Throwable, DroneTelemetry> handleError(String vehicleId) {
        return throwable -> {
            log.error("Failed to process telemetry for vehicle: {}", vehicleId, throwable);
            throw new RuntimeException(
                    "Telemetry processing failed for vehicle: " + vehicleId,
                    throwable
            );
        };
    }

}