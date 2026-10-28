package br.com.testetecnico.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.testetecnico.cadastro.client.NacionalidadeClient;
import br.com.testetecnico.cadastro.client.Previsao;
import com.jayway.jsonpath.JsonPath;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class FluxoV1Test extends PostgresBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @MockitoBean
    NacionalidadeClient nacionalidade;

    private String login() throws Exception {
        String corpo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + USUARIO + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(corpo, "$.token");
    }

    private String pessoa(String documento, String nome, String sobrenome) {
        return "{\"documento\":\"" + documento + "\",\"nome\":\"" + nome + "\",\"sobrenome\":\"" + sobrenome
                + "\",\"email\":\"" + nome.toLowerCase() + "@x.com\"}";
    }

    @Test
    void loginCriarListarPaginadoBuscarNacionalidadeExcluir() throws Exception {
        jdbc.update("delete from pessoa");
        when(nacionalidade.prever("Ana")).thenReturn(Optional.of(new Previsao("PT", 0.7)));

        mvc.perform(get("/api/v1/pessoas")).andExpect(status().isUnauthorized());
        String auth = login();

        mvc.perform(post("/api/v1/pessoas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(pessoa("529.982.247-25", "Ana", "Silva")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pessoas/52998224725"));
        mvc.perform(post("/api/v1/pessoas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(pessoa("390.533.447-05", "Bruno", "Alves")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/pessoas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(pessoa("52998224725", "Ana", "Silva")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DOCUMENTO_JA_CADASTRADO"));

        mvc.perform(get("/api/v1/pessoas").param("tamanho", "1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Ana"))
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/v1/pessoas").param("pagina", "2").param("tamanho", "1").header("Authorization", auth))
                .andExpect(jsonPath("$.itens[0].nome").value("Bruno"));

        mvc.perform(get("/api/v1/pessoas/52998224725").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.id", matchesPattern("[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}")))
                .andExpect(jsonPath("$.criadoEm", endsWith("-03:00")));
        mvc.perform(get("/api/v1/pessoas/52998224725/nacionalidade").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomePais").value("Portugal"));

        mvc.perform(delete("/api/v1/pessoas/52998224725").header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/pessoas/52998224725").header("Authorization", auth)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from pessoa", Integer.class)).isEqualTo(1);
    }

    @Test
    void rotasDoEnunciadoForamRemovidas() throws Exception {
        String auth = login();

        for (String rota : new String[] {"/list", "/list/52998224725", "/findNacionalityByPerson/52998224725"}) {
            mvc.perform(get(rota).header("Authorization", auth))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
        }
        mvc.perform(post("/registrarName").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(pessoa("529.982.247-25", "Ana", "Silva")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/list/52998224725").header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"" + USUARIO + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized()); // deixou de ser pública: sem token, nem chega a ser 404
    }
}
