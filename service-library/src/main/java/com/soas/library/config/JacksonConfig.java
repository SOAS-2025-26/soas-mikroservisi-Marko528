package com.soas.library.config;

import com.fasterxml.jackson.core.JsonGenerator;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Zajednicka podesavanja serijalizacije za sve mikroservise.
 *
 * Bez ovoga bi Jackson iznose tipa BigDecimal ispisivao u naucnoj notaciji
 * (npr. 0E-8 umesto 0.00000000), sto je korisniku necitljivo.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer plainBigDecimalCustomizer() {
        return builder -> builder.featuresToEnable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    }
}
