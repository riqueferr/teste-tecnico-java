package br.com.testetecnico.cadastro.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.testetecnico.cadastro.PostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.auth.rate-limit.tentativas=3")
class RateLimitLoginTest extends PostgresBase {

    @Autowired
    MockMvc mvc;

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String ERRADO = "{\"usuario\":\"admin\",\"senha\":\"errada\"}";
    private static final String CERTO = "{\"usuario\":\"admin\",\"senha\":\"admin123\"}";

    @Test
    void aQuartaTentativaViraMuitasTentativasMesmoPorUrlComPercentEncoding() throws Exception {
        // prova de que o MVC roteia a forma codificada: a senha certa autentica por /api/v1/auth/log%69n
        mvc.perform(postDoIp("/api/v1/auth/log%69n").contentType(MediaType.APPLICATION_JSON).content(CERTO))
                .andExpect(status().isOk());
        for (int i = 0; i < 2; i++) {
            mvc.perform(postDoIp(LOGIN).contentType(MediaType.APPLICATION_JSON).content(ERRADO))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(postDoIp("/api/v1/auth/log%69n").contentType(MediaType.APPLICATION_JSON).content(ERRADO))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("MUITAS_TENTATIVAS"));
    }

    /** POST com a URI crua (percent-encoded) e IP próprio, para não dividir o contador com os outros testes do contexto. */
    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postDoIp(String uri) {
        return post(java.net.URI.create(uri)).with(req -> {
            req.setRemoteAddr("10.9.9.9");
            return req;
        });
    }
}
