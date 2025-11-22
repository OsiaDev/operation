package co.cetad.umas.operation.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuración de la aplicación
 *
 * - @EnableAsync: Habilita el procesamiento asíncrono con @Async
 * - @EnableTransactionManagement: Habilita las transacciones declarativas
 */
@Configuration
@EnableAsync
@EnableTransactionManagement
public class ApplicationConfig {
    // Configuraciones generales de la aplicación
}