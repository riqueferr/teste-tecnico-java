package br.com.testetecnico.cadastro.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import br.com.testetecnico.cadastro.exception.MuitasTentativas;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

class LoginRateLimitFilterTest {

    private static final String LOGIN = "/api/v1/auth/login";

    static class RelogioMutavel extends Clock {
        Instant agora = Instant.parse("2026-10-27T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    RelogioMutavel relogio;
    HandlerExceptionResolver resolver;
    LoginRateLimitFilter filtro;
    AtomicInteger passaram;
    FilterChain cadeia;

    @BeforeEach
    void preparar() {
        relogio = new RelogioMutavel();
        resolver = mock(HandlerExceptionResolver.class);
        filtro = new LoginRateLimitFilter(new AuthProperties.RateLimit(3, Duration.ofSeconds(60)), relogio, resolver);
        passaram = new AtomicInteger();
        cadeia = (rq, rs) -> passaram.incrementAndGet();
    }

    private void tentar(String metodo, String caminho, String ip) throws Exception {
        var req = new MockHttpServletRequest(metodo, caminho);
        req.setRemoteAddr(ip);
        filtro.doFilter(req, new MockHttpServletResponse(), cadeia);
    }

    private void tentarLogin(String ip) throws Exception {
        tentar("POST", LOGIN, ip);
    }

    private static String ip(int i) {
        return "10." + (i / 65536) + "." + ((i / 256) % 256) + "." + (i % 256);
    }

    private long retryAfterDaUnicaRejeicao() {
        var capturada = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), capturada.capture());
        assertThat(capturada.getValue()).isInstanceOf(MuitasTentativas.class);
        return ((MuitasTentativas) capturada.getValue()).retryAfterSegundos();
    }

    /**
     * As 3 primeiras passam e a 4ª é a única rejeitada, com os segundos restantes da janela arredondados para cima
     * e nunca abaixo de 1 (senão 0 deixaria passar).
     */
    @ParameterizedTest(name = "{0} ns depois da 3ª tentativa -> Retry-After {1} s")
    @CsvSource({
            "20000000000, 40",       // 20 s
            "500000000, 60",         // restam 59,5 s -> 60
            "59500000000, 1",        // restam 0,5 s -> 1
            "59999500000, 1"})       // restam 0,5 ms: toMillis() = 0, o piso de 1 impede o fail-open
    void retryAfterArredondaParaCimaComPisoDeUmSegundo(long avancoEmNanos, long esperado) throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin("1.1.1.1");
        }
        relogio.agora = relogio.agora.plusNanos(avancoEmNanos);

        tentarLogin("1.1.1.1");

        assertThat(passaram.get()).isEqualTo(3);
        assertThat(retryAfterDaUnicaRejeicao()).isEqualTo(esperado);
    }

    @Test
    void depoisDaJanelaOContadorReinicia() throws Exception {
        for (int i = 0; i < 4; i++) {
            tentarLogin("1.1.1.1");
        }
        relogio.agora = relogio.agora.plusSeconds(60);

        tentarLogin("1.1.1.1");

        assertThat(passaram.get()).isEqualTo(4); // 3 antes do bloqueio + 1 depois da janela
        verify(resolver, times(1)).resolveException(any(), any(), any(), any());
    }

    @Test
    void ipsDiferentesTemJanelasIndependentes() throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin("1.1.1.1");
        }

        tentarLogin("2.2.2.2");

        assertThat(passaram.get()).isEqualTo(4);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    void outrasRotasEMetodosNaoContam() throws Exception {
        for (int i = 0; i < 10; i++) {
            tentar("GET", LOGIN, "1.1.1.1");
            tentar("POST", "/api/v1/pessoas", "1.1.1.1");
            tentar("POST", "/auth/login", "1.1.1.1"); // rota do enunciado, removida: não é mais login
        }

        assertThat(passaram.get()).isEqualTo(30);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    void urlComPercentEncodingNaoContornaOLimite() throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin("1.1.1.1");
        }

        tentar("POST", "/api/v1/auth/log%69n", "1.1.1.1");

        assertThat(passaram.get()).isEqualTo(3);
        retryAfterDaUnicaRejeicao();
    }

    @Test
    void comMuitosIpsAsJanelasAtivasNaoSaoDespejadas() throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin("1.1.1.1");
        }
        for (int i = 0; i < 10_001; i++) { // passa do limite de IPs guardados em memória
            tentarLogin(ip(i));
        }
        passaram.set(0);

        tentarLogin("1.1.1.1"); // a varredura roda, mas esta janela ainda vale: o limite não pode ser zerado

        assertThat(passaram.get()).isZero();
        retryAfterDaUnicaRejeicao();
    }

    /** A varredura só começa acima de 10 mil IPs guardados: no limite exato ninguém é removido, um a mais e tudo que expirou sai. */
    @ParameterizedTest(name = "{0} IPs + 1 tentativa depois da janela -> {1} guardados")
    @CsvSource({"10000, 10001", "10001, 1"})
    void asJanelasExpiradasSoSaemDaMemoriaAcimaDoLimiteDeIps(int ips, int guardados) throws Exception {
        for (int i = 0; i < ips; i++) {
            tentarLogin(ip(i));
        }
        relogio.agora = relogio.agora.plusSeconds(61);

        tentarLogin("9.9.9.9");

        Map<?, ?> janelas = (Map<?, ?>) ReflectionTestUtils.getField(filtro, "janelas");
        assertThat(janelas).hasSize(guardados);
    }
}
