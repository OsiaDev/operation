package co.cetad.umas.operation.infrastructure.persistence.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.ArrayList;

/**
 * Configuración de converters personalizados para R2DBC
 *
 * NOTA: Los ENUMs de PostgreSQL (mission_origin, mission_state) ahora se manejan
 * mediante EnumCodec en DatabaseConfig, por lo que NO necesitamos converters personalizados
 * para ellos. El EnumCodec maneja automáticamente la conversión entre los tipos ENUM
 * de PostgreSQL y los enums de Java.
 */
@Configuration
public class R2dbcConvertersConfig {

    /**
     * Registra converters personalizados para R2DBC
     * Actualmente vacío porque los ENUMs se manejan via EnumCodec
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, new ArrayList<>());
    }

}