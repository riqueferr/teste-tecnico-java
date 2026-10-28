package br.com.testetecnico.cadastro.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthPropertiesTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void janelaZeroOuNegativaEInvalidaPoisDesligariaOLimiteEmSilencio() {
        assertThat(validador.validate(new AuthProperties.RateLimit(5, Duration.ZERO))).hasSize(1);
        assertThat(validador.validate(new AuthProperties.RateLimit(5, Duration.ofSeconds(-30)))).hasSize(1);
    }

    @Test
    void janelaDeUmSegundoOuMaisEValida() {
        assertThat(validador.validate(new AuthProperties.RateLimit(5, Duration.ofSeconds(1)))).isEmpty();
    }

    @Test
    void tentativasPrecisamSerPeloMenosUma() {
        assertThat(validador.validate(new AuthProperties.RateLimit(0, Duration.ofSeconds(60)))).hasSize(1);
    }
}
