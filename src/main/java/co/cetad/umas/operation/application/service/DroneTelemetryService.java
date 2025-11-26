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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio que procesa telemetría de drones y auto-registra drones desconocidos
 *
 * FLUJO OPTIMIZADO:
 * 1. Guarda telemetría INMEDIATAMENTE (sin esperar verificación del dron)
 * 2. Paralelamente, verifica/crea el dron de forma asíncrona
 *
 * MEJORA: Usa locks por vehicleId para evitar race conditions al crear drones
 *
 * La telemetría NUNCA se bloquea esperando que exista el dron
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DroneTelemetryService implements EventProcessor<DroneTelemetry, TelemetryEvent> {

    private final DroneTelemetryRepository telemetryRepository;
    private final DroneRepository droneRepository;

    // Map de locks por vehicleId para sincronizar creación de drones
    // Evita que múltiples threads intenten crear el mismo dron simultáneamente
    private final Map<String, Lock> droneCreationLocks = new ConcurrentHashMap<>();

    /**
     * Procesa y almacena un evento de telemetría
     *
     * FLUJO OPTIMIZADO:
     * 1. Guarda telemetría INMEDIATAMENTE (no espera verificación del dron)
     * 2. Paralelamente, verifica/crea el dron de forma asíncrona con locks
     *
     * La telemetría NUNCA debe bloquearse esperando que exista el dron
     */
    @Override
    public CompletableFuture<DroneTelemetry> process(TelemetryEvent event) {
        log.debug("Processing telemetry for vehicle: {}", event.vehicleId());

        // Guardar telemetría INMEDIATAMENTE sin esperar nada
        CompletableFuture<DroneTelemetry> telemetrySaved = saveTelemetry(event)
                .exceptionally(throwable -> {
                    log.error("❌ Failed to save telemetry for vehicle: {}",
                            event.vehicleId(), throwable);
                    throw new TelemetrySaveException(
                            "Failed to save telemetry for vehicle: " + event.vehicleId(),
                            throwable
                    );
                });

        // Paralelamente, verificar/crear dron (no bloquea la telemetría)
        // MEJORA: Ahora con locks para evitar race conditions
        ensureDroneExistsAsync(event.vehicleId());

        return telemetrySaved;
    }

    /**
     * Verifica/crea dron de forma completamente asíncrona
     * CON SINCRONIZACIÓN para evitar que múltiples threads creen el mismo dron
     *
     * Este proceso NO debe bloquear el guardado de telemetría
     */
    private void ensureDroneExistsAsync(String vehicleId) {
        // Obtener o crear lock para este vehicleId
        Lock lock = droneCreationLocks.computeIfAbsent(vehicleId, k -> new ReentrantLock());

        CompletableFuture.runAsync(() -> {
            // Intentar adquirir lock SIN BLOQUEAR
            // Si otro thread está creando el dron, este thread simplemente sale
            if (lock.tryLock()) {
                try {
                    // Tenemos el lock, proceder con verificación/creación
                    ensureDroneExists(vehicleId)
                            .thenAccept(drone ->
                                    log.debug("✅ Drone verified/created: {} for vehicleId: {}",
                                            drone.id(), drone.vehicleId())
                            )
                            .exceptionally(throwable -> {
                                log.warn("⚠️ Failed to verify/create drone for vehicleId: {} " +
                                        "(telemetry was saved anyway)", vehicleId, throwable);
                                return null;
                            })
                            .join(); // Esperar a que termine SOLO este thread
                } finally {
                    lock.unlock();
                    // Limpiar lock si ya no es necesario
                    droneCreationLocks.remove(vehicleId, lock);
                }
            } else {
                // Otro thread ya está creando el dron, simplemente salir
                log.debug("🔒 Another thread is already creating drone for vehicleId: {}", vehicleId);
            }
        });
    }

    /**
     * Asegura que el dron existe en la base de datos
     * Si no existe, lo crea automáticamente con valores predeterminados
     *
     * MEJORA: Verificación adicional antes de crear para evitar duplicados
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
     * MEJORA: Manejo de excepción de duplicado (por si acaso otro thread lo creó)
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
     * @return Drone creado o existente si otro thread ya lo creó
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
                    // Si falla por duplicado, intentar buscar el dron (otro thread lo creó)
                    if (throwable.getMessage() != null &&
                            (throwable.getMessage().contains("unique") ||
                                    throwable.getMessage().contains("duplicate"))) {
                        log.info("ℹ️ Drone already exists for vehicleId: {} (created by another thread)",
                                vehicleId);

                        // Buscar el dron que otro thread creó
                        return droneRepository.findByVehicleId(vehicleId)
                                .thenApply(opt -> opt.orElseThrow(() ->
                                        new DroneCreationException(
                                                "Drone should exist but couldn't be found for vehicleId: " + vehicleId,
                                                throwable
                                        )
                                ))
                                .join();
                    }

                    // Otro tipo de error
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
     * Excepción personalizada para errores de guardado de telemetría
     */
    public static class TelemetrySaveException extends RuntimeException {
        public TelemetrySaveException(String message, Throwable cause) {
            super(message, cause);
        }
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