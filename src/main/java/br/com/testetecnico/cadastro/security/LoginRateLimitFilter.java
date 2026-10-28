package br.com.testetecnico.cadastro.security;

import br.com.testetecnico.cadastro.exception.MuitasTentativas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

// ponytail: estado em memória (uma instância) e janela fixa; IP = remoteAddr, sem confiar em X-Forwarded-For.
// Com réplicas ou proxy, limitar no gateway ou usar store compartilhado.
@RequiredArgsConstructor
class LoginRateLimitFilter extends OncePerRequestFilter {

    // caminho decodificado, como no authorizeHttpRequests: "/api/v1/auth/log%69n" também é o login
    private static final RequestMatcher LOGIN =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/login");
    private static final int LIMITE_DE_IPS_NA_MEMORIA = 10_000;

    private record Janela(Instant inicio, int tentativas) {
    }

    private final AuthProperties.RateLimit limite;
    private final Clock relogio;
    private final HandlerExceptionResolver resolver;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long espera = registrar(request.getRemoteAddr(), relogio.instant());
        if (espera > 0) {
            resolver.resolveException(request, response, null, new MuitasTentativas(espera));
            return;
        }
        chain.doFilter(request, response);
    }

    private long registrar(String ip, Instant agora) {
        if (janelas.size() > LIMITE_DE_IPS_NA_MEMORIA) {
            janelas.values().removeIf(j -> expirou(j, agora));
        }
        long[] espera = {0};
        janelas.compute(ip, (chave, atual) -> {
            if (atual == null || expirou(atual, agora)) {
                return new Janela(agora, 1);
            }
            if (atual.tentativas() >= limite.tentativas()) {
                Duration restante = Duration.between(agora, atual.inicio().plus(limite.janela()));
                espera[0] = Math.max(1, (restante.toMillis() + 999) / 1000);
                return atual;
            }
            return new Janela(atual.inicio(), atual.tentativas() + 1);
        });
        return espera[0];
    }

    private boolean expirou(Janela janela, Instant agora) {
        return !agora.isBefore(janela.inicio().plus(limite.janela()));
    }
}
