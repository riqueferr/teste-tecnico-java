package br.com.testetecnico.cadastro.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
        @NotBlank String user,
        @NotBlank String passwordHash,
        @NotBlank @Size(min = 32, message = "o segredo JWT precisa ter no mínimo 32 caracteres") String jwtSecret,
        @NotNull Duration tokenTtl,
        @NotNull @Valid RateLimit rateLimit) {

    public record RateLimit(@Min(1) int tentativas, @NotNull @DurationMin(seconds = 1) Duration janela) {
    }
}
