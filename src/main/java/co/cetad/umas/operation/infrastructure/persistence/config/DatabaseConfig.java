package co.cetad.umas.operation.infrastructure.persistence.config;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Configuración de conexión a PostgreSQL con soporte para tipos ENUM personalizados
 *
 * IMPORTANTE:
 * - EnumCodec (aquí) maneja la conversión a nivel de driver R2DBC
 * - R2dbcConvertersConfig maneja la conversión a nivel de Spring Data R2DBC
 *
 * Ambos son necesarios para que los ENUMs funcionen correctamente:
 * - EnumCodec: Permite que el driver R2DBC entienda los tipos ENUM de PostgreSQL
 * - Converters: Permiten que Spring Data R2DBC convierta entre Java ENUMs y Strings
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.r2dbc.host:localhost}")
    private String host;

    @Value("${spring.r2dbc.port:5432}")
    private int port;

    @Value("${spring.r2dbc.database:drone_notifications}")
    private String database;

    @Value("${spring.r2dbc.username:drone_user}")
    private String username;

    @Value("${spring.r2dbc.password:drone_pass}")
    private String password;

    /**
     * Configura el ConnectionFactory con soporte para ENUMs personalizados de PostgreSQL
     *
     * Los tipos ENUM de PostgreSQL (mission_origin, mission_state) se registran
     * mediante EnumCodec para que R2DBC los maneje correctamente durante INSERT/UPDATE
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        PostgresqlConnectionConfiguration config = PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                // ✅ Registrar tipos ENUM personalizados de PostgreSQL
                .codecRegistrar(EnumCodec.builder()
                        .withEnum("mission_origin", MissionOrigin.class)
                        .withEnum("mission_state", MissionState.class)
                        .build())
                .build();

        return new PostgresqlConnectionFactory(config);
    }

    @Bean
    public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                .namedParameters(true)
                .build();
    }

    @Bean
    public R2dbcTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

}