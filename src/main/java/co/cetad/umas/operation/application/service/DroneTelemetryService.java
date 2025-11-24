package co.cetad.umas.operation.application.service;

import co.cetad.umas.operation.domain.model.entity.DroneTelemetry;
import co.cetad.umas.operation.domain.model.vo.Drone;
import co.cetad.umas.operation.domain.model.vo.GeoLocation;
import co.cetad.umas.operation.domain.model.vo.TelemetryEvent;
import co.cetad.umas.operation.domain.model.vo.TelemetryMetrics;
import co.cetad.umas.operation.domain.ports.in.EventProcessor;
import co.cetad.umas.operation.domain.ports.out.DroneRepository;
import co.cetad.umas.operation.domain.ports.out.DroneTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Servicio que procesa telemetría de drones y auto-registra drones desconocidos
 *
 * Responsabilidades:
 * 1. Verificar si el dron existe en la base de datos
 * 2. Si no existe, crear automáticamente con valores predeterminados
 * 3. Transformar evento a modelo de dominio
 * 4. Persistir telemetría en base de datos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneTelemetryService implements EventProcessor<DroneTelemetry, TelemetryEvent> {

    private final DroneTelemetryRepository telemetryRepository;
    private final DroneRepository droneRepository;

    /**
     * Procesa y almacena un evento de telemetría
     * Si el dron no existe, lo crea automáticamente antes de guardar la telemetría
     */
    @Override
    public CompletableFuture<DroneTelemetry> process(TelemetryEvent event) {
        log.debug("Processing telemetry for vehicle: {}", event.vehicleId());

        return ensureDroneExists(event.vehicleId())
                .thenCompose(drone -> {
                    log.debug("Drone verified/created: {} for vehicleId: {}",
                            drone.id(), drone.vehicleId());
                    return saveTelemetry(event);
                })
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
     * Asegura que el dron existe en la base de datos
     * Si no existe, lo crea automáticamente con valores predeterminados
     *
     * @param vehicleId ID del vehículo de la telemetría
     * @return Drone existente o recién creado
     */
    private CompletableFuture<Drone> ensureDroneExists(String vehicleId) {
        return droneRepository.findByVehicleId(vehicleId)
                .thenCompose(droneOpt -> {
                    if (droneOpt.isPresent()) {
                        // Dron ya existe
                        log.debug("Drone already exists for vehicleId: {}", vehicleId);
                        return CompletableFuture.completedFuture(droneOpt.get());
                    } else {
                        // Dron no existe, crear uno nuevo
                        log.info("🆕 Drone not found for vehicleId: {}. Creating unknown drone...",
                                vehicleId);
                        return createUnknownDrone(vehicleId);
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Error ensuring drone exists for vehicleId: {}",
                            vehicleId, throwable);
                    throw new DroneVerificationException(
                            "Failed to verify/create drone for vehicleId: " + vehicleId,
                            throwable
                    );
                });
    }

    /**
     * Crea un dron desconocido con valores predeterminados
     *
     * Valores por defecto:
     * - name: vehicleId
     * - model: "No Reconocida"
     * - description: "Dron registrado automáticamente por telemetría"
     * - serialNumber: "00000000"
     * - status: ACTIVE
     * - flightHours: 0.00
     *
     * @param vehicleId ID del vehículo
     * @return Drone creado
     */
    private CompletableFuture<Drone> createUnknownDrone(String vehicleId) {
        return CompletableFuture
                .supplyAsync(() -> Drone.createUnknown(vehicleId))
                .thenCompose(droneRepository::save)
                .thenApply(savedDrone -> {
                    log.info("✅ Unknown drone created successfully: {} for vehicleId: {}",
                            savedDrone.id(), savedDrone.vehicleId());
                    return savedDrone;
                })
                .exceptionally(throwable -> {
                    log.error("❌ Failed to create unknown drone for vehicleId: {}",
                            vehicleId, throwable);
                    throw new DroneCreationException(
                            "Failed to create unknown drone for vehicleId: " + vehicleId,
                            throwable
                    );
                });
    }

    /**
     * Guarda la telemetría en la base de datos
     */
    private CompletableFuture<DroneTelemetry> saveTelemetry(TelemetryEvent event) {
        return CompletableFuture
                .supplyAsync(() -> transformToDomain(event))
                .thenCompose(telemetryRepository::save);
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

    /**
     * Excepción personalizada para errores de verificación de drones
     */
    public static class DroneVerificationException extends RuntimeException {
        public DroneVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción personalizada para errores de creación de drones
     */
    public static class DroneCreationException extends RuntimeException {
        public DroneCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}