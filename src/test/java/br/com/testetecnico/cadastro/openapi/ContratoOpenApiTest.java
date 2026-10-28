package br.com.testetecnico.cadastro.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.testetecnico.cadastro.PostgresBase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ContratoOpenApiTest extends PostgresBase {

    @Autowired
    MockMvc mvc;

    private String v1;

    private void carregar() throws Exception {
        if (v1 == null) {
            v1 = mvc.perform(get("/v3/api-docs/v1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        }
    }

    private static final Map<String, List<String>> ESPERADO = Map.of(
            "post:/api/v1/auth/login", List.of("200", "400", "401", "429", "500"),
            "post:/api/v1/pessoas", List.of("201", "400", "401", "409", "500"),
            "get:/api/v1/pessoas", List.of("200", "400", "401", "500"),
            "get:/api/v1/pessoas/{documento}", List.of("200", "400", "401", "404", "500"),
            "delete:/api/v1/pessoas/{documento}", List.of("204", "400", "401", "404", "500"),
            "get:/api/v1/pessoas/{documento}/nacionalidade", List.of("200", "400", "401", "404", "502", "500"));

    @Test
    void cadaOperacaoDeclaraTodasAsRespostasEsperadas() throws Exception {
        carregar();
        for (var entrada : ESPERADO.entrySet()) {
            String metodo = entrada.getKey().split(":")[0];
            String caminho = entrada.getKey().split(":", 2)[1];
            Map<String, Object> respostas = JsonPath.read(v1, "$.paths['" + caminho + "']." + metodo + ".responses");
            assertThat(respostas.keySet()).as(entrada.getKey()).containsAll(entrada.getValue());
        }
        // springdoc acrescenta um 200 padrão quando o 2xx não é explícito; o POST devolve só 201
        Map<String, Object> criar = JsonPath.read(v1, "$.paths['/api/v1/pessoas'].post.responses");
        assertThat(criar).doesNotContainKey("200");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> respostasPorOperacao() {
        List<Map<String, Object>> operacoes = JsonPath.read(v1, "$.paths.*.*");
        return operacoes.stream().map(op -> (Map<String, Object>) op.get("responses")).toList();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sucessosComCorpoSaoApplicationJsonENuncaCoringaEErrosApontamParaOEsquemaProblema() throws Exception {
        carregar();
        int sucessos = 0;
        int erros = 0;
        for (Map<String, Object> respostas : respostasPorOperacao()) {
            for (var r : respostas.entrySet()) {
                int codigo = Integer.parseInt(r.getKey());
                if (codigo == 204) {
                    continue;
                }
                Map<String, Object> content = (Map<String, Object>) ((Map<String, Object>) r.getValue()).get("content");
                assertThat(content).as("content do " + r.getKey()).isNotNull();
                if (codigo < 400) {
                    assertThat(content.keySet()).as("media types do " + r.getKey()).contains("application/json").doesNotContain("*/*");
                    sucessos++;
                } else {
                    assertThat(content.keySet()).as("media types do " + r.getKey()).containsExactly("application/problem+json");
                    Map<String, Object> schema = (Map<String, Object>) ((Map<String, Object>) content.get("application/problem+json")).get("schema");
                    assertThat(schema.get("$ref")).as("$ref do " + r.getKey()).isEqualTo("#/components/schemas/Problema");
                    erros++;
                }
            }
        }
        assertThat(sucessos).isEqualTo(5); // login 200, criar 201, listar 200, buscar 200, nacionalidade 200
        assertThat(erros).isGreaterThan(10);
        Map<String, Object> propriedades = JsonPath.read(v1, "$.components.schemas.Problema.properties");
        assertThat(propriedades.keySet()).contains("type", "title", "status", "detail", "instance", "codigo", "erros");
    }

    @Test
    void metadadosVersaoTagsSummarySegurancaECabecalhosLegiveis() throws Exception {
        carregar();
        assertThat(JsonPath.<String>read(v1, "$.info.version")).isEqualTo("1.0.0");
        assertThat(JsonPath.<List<String>>read(v1, "$.paths.*.*.summary")).hasSize(6).allSatisfy(s -> assertThat(s).isNotBlank());
        assertThat(JsonPath.<List<List<String>>>read(v1, "$.paths.*.*.tags"))
                .allSatisfy(t -> assertThat(t).hasSize(1).first().isIn("Pessoas", "Autenticação"));
        assertThat(JsonPath.<List<Object>>read(v1, "$.paths['/api/v1/auth/login'].post.security")).isEmpty();
        assertThat(JsonPath.<Map<String, Object>>read(v1, "$.paths['/api/v1/auth/login'].post.responses['429'].headers"))
                .containsKey("Retry-After");
        assertThat(JsonPath.<Map<String, Object>>read(v1, "$.paths['/api/v1/pessoas'].post.responses['201'].headers"))
                .containsKey("Location");
    }

    @Test
    void swaggerUiTemSoOGrupoV1ComoPrimario() throws Exception {
        String config = mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(config, "$.urls[*].name")).containsExactly("v1");
        assertThat(JsonPath.<String>read(config, "$['urls.primaryName']")).isEqualTo("v1");
    }
}
