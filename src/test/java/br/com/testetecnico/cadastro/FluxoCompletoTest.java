package br.com.testetecnico.cadastro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Autenticação ponta a ponta (JWT real, Postgres real); o fluxo de negócio, com o login real, fica em {@link FluxoV1Test}. */
@AutoConfigureMockMvc
class FluxoCompletoTest extends PostgresBase {

    private static final String LISTA = "/api/v1/pessoas";

    @Autowired
    MockMvc mvc;
    @Value("${app.auth.jwt-secret}")
    String segredo;

    @Test
    void paginaESwaggerSaoPublicosEApiNao() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get(LISTA)).andExpect(status().isUnauthorized());
    }

    /** JWT HS256 assinado à mão, para forjar tokens que o login nunca emitiria. */
    private String bearer(String segredoDeAssinatura, Instant expiraEm) {
        var chave = new SecretKeySpec(segredoDeAssinatura.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var claims = JwtClaimsSet.builder().subject(USUARIO)
                .issuedAt(expiraEm.minusSeconds(7200)).expiresAt(expiraEm).build();
        var jwt = new NimbusJwtEncoder(new ImmutableSecret<>(chave))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return "Bearer " + jwt.getTokenValue();
    }

    @ParameterizedTest(name = "{0} -> {3}")
    @CsvSource(delimiter = '|', value = {
            "controle: segredo certo e exp no futuro | true | 3600 | 200", // prova que o helper forja um token aceito
            "token expirado | true | -3600 | 401",
            "assinado com outra chave | false | 3600 | 401"})
    void tokenForjadoEhAceitoSoComSegredoCertoEDentroDaValidade(String caso, boolean segredoCerto, long expiraEmSegundos,
            int esperado) throws Exception {
        String chave = segredoCerto ? segredo : "outro-segredo-de-teste-com-32-caracteres!";

        mvc.perform(get(LISTA).header("Authorization", bearer(chave, Instant.now().plusSeconds(expiraEmSegundos))))
                .andExpect(status().is(esperado));
    }
}
