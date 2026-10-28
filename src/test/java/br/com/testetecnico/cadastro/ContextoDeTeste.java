package br.com.testetecnico.cadastro;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("test")
public abstract class ContextoDeTeste {

    public static final String USUARIO = "admin";
    public static final String SENHA = "admin123";

    @DynamicPropertySource
    static void credenciais(DynamicPropertyRegistry registry) {
        registry.add("app.auth.password-hash", () -> new BCryptPasswordEncoder().encode(SENHA));
    }
}
