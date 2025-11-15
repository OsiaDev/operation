package co.cetad.umas.operation.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de métricas para monitoreo y observabilidad
 *
 * Proporciona:
 * - Contadores de eventos procesados
 * - Métricas de latencia
 * - Tasas de error
 * - Estadísticas por vehículo
 *
 * TODO: Integrar con Micrometer/Prometheus para métricas reales
 */
@Slf4j
@Service
public class TelemetryMetricsService {

    // Contadores globales
    private final AtomicLong totalEventsReceived = new AtomicLong(0);
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsInvalid = new AtomicLong(0);

    // Métricas por vehículo
    private final Map<String, VehicleMetrics> vehicleMetricsMap = new ConcurrentHashMap<>();

    // Métricas de latencia
    private final Map<String, Long> processingStartTimes = new ConcurrentHashMap<>();

    /**
     * Registra el inicio del procesamiento de un evento
     */
    public void recordEventReceived(String vehicleId) {
        totalEventsReceived.incrementAndGet();
        vehicleMetricsMap
                .computeIfAbsent(vehicleId, VehicleMetrics::new)
                .recordReceived();

        processingStartTimes.put(vehicleId, System.currentTimeMillis());
    }

    /**
     * Registra el éxito del procesamiento de un evento
     */
    public void recordEventProcessed(String vehicleId) {
        totalEventsProcessed.incrementAndGet();

        VehicleMetrics metrics = vehicleMetricsMap.get(vehicleId);
        if (metrics != null) {
            metrics.recordProcessed();

            // Calcular latencia
            Long startTime = processingStartTimes.remove(vehicleId);
            if (startTime != null) {
                long latency = System.currentTimeMillis() - startTime;
                metrics.recordLatency(latency);

                if (latency > 5000) {
                    log.warn("High latency detected for vehicle {}: {}ms", vehicleId, latency);
                }
            }
        }
    }

    /**
     * Registra un fallo en el procesamiento
     */
    public void recordEventFailed(String vehicleId, String errorType) {
        totalEventsFailed.incrementAndGet();

        VehicleMetrics metrics = vehicleMetricsMap.get(vehicleId);
        if (metrics != null) {
            metrics.recordFailed(errorType);
        }

        processingStartTimes.remove(vehicleId);
    }

    /**
     * Registra un evento inválido
     */
    public void recordEventInvalid(String reason) {
        totalEventsInvalid.incrementAndGet();
        log.warn("Invalid event detected: {}", reason);
    }

    /**
     * Registra una alerta generada
     */
    public void recordAlert(String vehicleId, String alertType) {
        VehicleMetrics metrics = vehicleMetricsMap.get(vehicleId);
        if (metrics != null) {
            metrics.recordAlert(alertType);
        }
        log.info("Alert recorded for vehicle {}: {}", vehicleId, alertType);
    }

    /**
     * Obtiene las métricas globales
     */
    public GlobalMetrics getGlobalMetrics() {
        return new GlobalMetrics(
                totalEventsReceived.get(),
                totalEventsProcessed.get(),
                totalEventsFailed.get(),
                totalEventsInvalid.get(),
                calculateSuccessRate(),
                vehicleMetricsMap.size()
        );
    }

    /**
     * Obtiene las métricas de un vehículo específico
     */
    public VehicleMetrics getVehicleMetrics(String vehicleId) {
        return vehicleMetricsMap.get(vehicleId);
    }

    /**
     * Obtiene todas las métricas de vehículos
     */
    public Map<String, VehicleMetrics> getAllVehicleMetrics() {
        return Map.copyOf(vehicleMetricsMap);
    }

    /**
     * Calcula la tasa de éxito
     */
    private double calculateSuccessRate() {
        long total = totalEventsReceived.get();
        if (total == 0) return 0.0;
        return (double) totalEventsProcessed.get() / total * 100;
    }

    /**
     * Limpia métricas antiguas (ejecutar periódicamente)
     */
    public void cleanupOldMetrics(Duration maxAge) {
        LocalDateTime cutoff = LocalDateTime.now().minus(maxAge);
        vehicleMetricsMap.entrySet().removeIf(entry ->
                entry.getValue().getLastUpdate().isBefore(cutoff)
        );
        log.info("Cleaned up old metrics. Active vehicles: {}", vehicleMetricsMap.size());
    }

    /**
     * Métricas globales del sistema
     */
    public record GlobalMetrics(
            long totalEventsReceived,
            long totalEventsProcessed,
            long totalEventsFailed,
            long totalEventsInvalid,
            double successRate,
            int activeVehicles
    ) {}

    /**
     * Métricas por vehículo
     */
    public static class VehicleMetrics {
        private final String vehicleId;
        private final AtomicLong eventsReceived = new AtomicLong(0);
        private final AtomicLong eventsProcessed = new AtomicLong(0);
        private final AtomicLong eventsFailed = new AtomicLong(0);
        private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> alertsByType = new ConcurrentHashMap<>();

        // Métricas de latencia
        private volatile long minLatency = Long.MAX_VALUE;
        private volatile long maxLatency = 0;
        private volatile long totalLatency = 0;
        private volatile long latencyCount = 0;

        private volatile LocalDateTime firstEvent;
        private volatile LocalDateTime lastUpdate;

        public VehicleMetrics(String vehicleId) {
            this.vehicleId = vehicleId;
            this.firstEvent = LocalDateTime.now();
            this.lastUpdate = LocalDateTime.now();
        }

        public void recordReceived() {
            eventsReceived.incrementAndGet();
            if (firstEvent == null) {
                firstEvent = LocalDateTime.now();
            }
            lastUpdate = LocalDateTime.now();
        }

        public void recordProcessed() {
            eventsProcessed.incrementAndGet();
            lastUpdate = LocalDateTime.now();
        }

        public void recordFailed(String errorType) {
            eventsFailed.incrementAndGet();
            errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0))
                    .incrementAndGet();
            lastUpdate = LocalDateTime.now();
        }

        public void recordLatency(long latency) {
            if (latency < minLatency) {
                minLatency = latency;
            }
            if (latency > maxLatency) {
                maxLatency = latency;
            }
            totalLatency += latency;
            latencyCount++;
        }

        public void recordAlert(String alertType) {
            alertsByType.computeIfAbsent(alertType, k -> new AtomicLong(0))
                    .incrementAndGet();
            lastUpdate = LocalDateTime.now();
        }

        // Getters
        public String getVehicleId() { return vehicleId; }
        public long getEventsReceived() { return eventsReceived.get(); }
        public long getEventsProcessed() { return eventsProcessed.get(); }
        public long getEventsFailed() { return eventsFailed.get(); }
        public Map<String, AtomicLong> getErrorsByType() { return Map.copyOf(errorsByType); }
        public Map<String, AtomicLong> getAlertsByType() { return Map.copyOf(alertsByType); }
        public LocalDateTime getLastUpdate() { return lastUpdate; }
        public LocalDateTime getFirstEvent() { return firstEvent; }

        public long getMinLatency() { return minLatency == Long.MAX_VALUE ? 0 : minLatency; }
        public long getMaxLatency() { return maxLatency; }
        public long getAverageLatency() {
            return latencyCount == 0 ? 0 : totalLatency / latencyCount;
        }

        public double getSuccessRate() {
            long total = eventsReceived.get();
            return total == 0 ? 0.0 : (double) eventsProcessed.get() / total * 100;
        }

        @Override
        public String toString() {
            return String.format(
                    "VehicleMetrics[id=%s, received=%d, processed=%d, failed=%d, " +
                            "successRate=%.2f%%, avgLatency=%dms, lastUpdate=%s]",
                    vehicleId, eventsReceived.get(), eventsProcessed.get(),
                    eventsFailed.get(), getSuccessRate(), getAverageLatency(), lastUpdate
            );
        }
    }

}