package br.com.testetecnico.cadastro.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(AuthProperties.class) // necessário nos slices @WebMvcTest, que não escaneiam @ConfigurationProperties
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, AuthenticationEntryPoint problemaEntryPoint,
                                AccessDeniedHandler problemaAcessoNegado, AuthProperties props, Clock relogio,
                                @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // API stateless com Bearer token; sem cookie de sessão
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/index.html", "/api/v1/auth/login", "/actuator/health",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problemaEntryPoint)
                        .accessDeniedHandler(problemaAcessoNegado))
                .oauth2ResourceServer(o -> o
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problemaEntryPoint)
                        .accessDeniedHandler(problemaAcessoNegado))
                // criado aqui, e não como @Bean, para o Boot não registrá-lo também como filtro de servlet
                .addFilterBefore(new LoginRateLimitFilter(props.rateLimit(), relogio, resolver),
                        BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationEntryPoint problemaEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        var bearer = new BearerTokenAuthenticationEntryPoint();
        return (req, res, ex) -> {
            bearer.commence(req, res, ex);
            resolver.resolveException(req, res, null, ex);
        };
    }

    @Bean
    AccessDeniedHandler problemaAcessoNegado(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return (req, res, ex) -> resolver.resolveException(req, res, null, ex);
    }

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties props) {
        return NimbusJwtDecoder.withSecretKey(chave(props)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(AuthProperties props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(props)));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static SecretKey chave(AuthProperties props) {
        return new SecretKeySpec(props.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
