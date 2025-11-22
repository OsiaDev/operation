package co.cetad.umas.operation.infrastructure.persistence.config;

import co.cetad.umas.operation.domain.model.entity.MissionOrigin;
import co.cetad.umas.operation.domain.model.entity.MissionState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de converters personalizados para R2DBC
 * Necesario para manejar ENUMs de PostgreSQL correctamente
 *
 * Los ENUMs de PostgreSQL (mission_origin, mission_state) requieren
 * conversión explícita para que R2DBC los maneje correctamente
 *
 * IMPORTANTE: PostgreSQL tiene tipos ENUM personalizados (mission_origin, mission_state)
 * que requieren estos converters para funcionar con Spring Data R2DBC
 */
@Configuration
public class R2dbcConvertersConfig {

    /**
     * Registra los converters personalizados para R2DBC
     * Necesario para que R2DBC pueda mapear correctamente los ENUMs de Java
     * a los tipos ENUM de PostgreSQL
     */
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        // Converters para MissionOrigin
        converters.add(new MissionOriginWritingConverter());
        converters.add(new MissionOriginReadingConverter());

        // Converters para MissionState
        converters.add(new MissionStateWritingConverter());
        converters.add(new MissionStateReadingConverter());

        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    /**
     * Converter para escribir MissionOrigin a PostgreSQL
     * Convierte el enum Java al nombre del enum para PostgreSQL
     */
    @WritingConverter
    public static class MissionOriginWritingConverter implements Converter<MissionOrigin, String> {
        @Override
        public String convert(MissionOrigin source) {
            return source.name();
        }
    }

    /**
     * Converter para leer MissionOrigin desde PostgreSQL
     * Convierte el valor String del enum PostgreSQL al enum Java
     */
    @ReadingConverter
    public static class MissionOriginReadingConverter implements Converter<String, MissionOrigin> {
        @Override
        public MissionOrigin convert(String source) {
            return MissionOrigin.valueOf(source);
        }
    }

    /**
     * Converter para escribir MissionState a PostgreSQL
     * Convierte el enum Java al nombre del enum para PostgreSQL
     */
    @WritingConverter
    public static class MissionStateWritingConverter implements Converter<MissionState, String> {
        @Override
        public String convert(MissionState source) {
            return source.name();
        }
    }

    /**
     * Converter para leer MissionState desde PostgreSQL
     * Convierte el valor String del enum PostgreSQL al enum Java
     */
    @ReadingConverter
    public static class MissionStateReadingConverter implements Converter<String, MissionState> {
        @Override
        public MissionState convert(String source) {
            return MissionState.valueOf(source);
        }
    }

}