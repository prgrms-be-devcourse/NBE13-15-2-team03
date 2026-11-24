package com.kdt.reservation.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    @Bean
    public JsonMapperBuilderCustomizer dateTimeFormatCustomizer() {
        return builder -> builder.withConfigOverride(
                LocalDateTime.class,
                override -> override.setFormat(JsonFormat.Value.forPattern(DATE_TIME_PATTERN))
        );
    }
}