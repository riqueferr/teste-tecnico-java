package br.com.testetecnico.cadastro.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.testetecnico.cadastro.ContextoDeTeste;
import br.com.testetecnico.cadastro.security.SecurityConfig;
import br.com.testetecnico.cadastro.service.AutenticacaoService;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AuthV1Controller.class)
@Import({SecurityConfig.class, AutenticacaoService.class})
class AuthV1ControllerTest extends ContextoDeTeste {

    @Autowired
    MockMvc mvc;

    private ResultActions login(String json) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void credenciaisCorretasDevolvemTokenQueExpiraUmaHoraDepois() throws Exception {
        var antes = Instant.now();
        String corpo = login("{\"usuario\":\"admin\",\"senha\":\"admin123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Instant expiraEm = Instant.parse(JsonPath.read(corpo, "$.expiraEm"));

        assertThat(expiraEm).isBetween(antes.plus(Duration.ofHours(1)), Instant.now().plus(Duration.ofHours(1)));
    }

    @ParameterizedTest
    @CsvSource({"admin,errada", "outro,admin123"}) // erra só a senha / erra só o usuário
    void credenciaisErradasRetornam401ComProblemJson(String usuario, String senha) throws Exception {
        login("{\"usuario\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void camposEmBrancoRetornam400() throws Exception {
        login("{\"usuario\":\"\",\"senha\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"));
    }
}
