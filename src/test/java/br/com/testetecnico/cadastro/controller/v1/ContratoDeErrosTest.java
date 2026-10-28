package br.com.testetecnico.cadastro.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.testetecnico.cadastro.ContextoDeTeste;
import br.com.testetecnico.cadastro.exception.DocumentoJaCadastrado;
import br.com.testetecnico.cadastro.exception.NacionalidadeIndisponivel;
import br.com.testetecnico.cadastro.exception.PessoaNaoEncontrada;
import br.com.testetecnico.cadastro.exception.PrevisaoNaoEncontrada;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.security.SecurityConfig;
import br.com.testetecnico.cadastro.service.NacionalidadeService;
import br.com.testetecnico.cadastro.service.Pagina;
import br.com.testetecnico.cadastro.service.PessoaService;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(controllers = {PessoaV1Controller.class, ContratoDeErrosTest.ControllerDeTeste.class})
@Import({SecurityConfig.class, ContratoDeErrosTest.ControllerDeTeste.class})
class ContratoDeErrosTest extends ContextoDeTeste {

    /** Rotas só de teste, para provocar erros que o app real não produz sozinho. */
    @RestController
    static class ControllerDeTeste {
        @GetMapping("/teste/negado")
        void negado() {
            throw new AccessDeniedException("sem permissão");
        }

        @GetMapping("/teste/grande")
        void grande() {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE);
        }

        @GetMapping("/teste/explode")
        void explode() {
            throw new IllegalStateException("segredo-interno-123");
        }

        @GetMapping("/teste/nao-serializa")
        Object naoSerializa() {
            return new Object() {
                public String getQuebra() {
                    throw new IllegalStateException("falha-de-serializacao-xyz");
                }
            };
        }
    }

    private static final String PESSOAS = "/api/v1/pessoas";
    private static final String CPF = "52998224725";
    private static final String JSON_PESSOA =
            "{\"documento\":\"%s\",\"nome\":\"%s\",\"sobrenome\":\"S\",\"email\":\"%s\"}";

    @Autowired
    MockMvc mvc;
    @Autowired
    AccessDeniedHandler acessoNegado;
    @MockitoBean
    PessoaService pessoas;
    @MockitoBean
    NacionalidadeService nacionalidade;

    /** Uma linha do contrato: a requisição, o problema esperado e o que for particular do caso. */
    record Caso(MockHttpServletRequestBuilder requisicao, int status, String codigo, String titulo,
                Consumer<ContratoDeErrosTest> preparo, ResultMatcher... extras) {
    }

    private static final Consumer<ContratoDeErrosTest> NADA = t -> {
    };

    private static MockHttpServletRequestBuilder postJson(String corpo) {
        return post(PESSOAS).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    static Stream<Arguments> casos() {
        return Stream.of(
                argumentSet("sem token → 401, com WWW-Authenticate",
                        new Caso(get(PESSOAS), 401, "NAO_AUTENTICADO", "Não autenticado", NADA,
                                header().string("WWW-Authenticate", startsWith("Bearer")),
                                jsonPath("$.detail").value("Credenciais ausentes, inválidas ou expiradas"))),
                argumentSet("token inválido → 401",
                        new Caso(get(PESSOAS).header("Authorization", "Bearer lixo"), 401, "NAO_AUTENTICADO",
                                "Não autenticado", NADA)),
                argumentSet("acesso negado → 403",
                        new Caso(get("/teste/negado").with(jwt()), 403, "ACESSO_NEGADO", "Acesso negado", NADA)),
                argumentSet("rota inexistente → 404",
                        new Caso(get("/nao-existe").with(jwt()), 404, "RECURSO_NAO_ENCONTRADO", "Não encontrado", NADA)),
                argumentSet("método não permitido → 405, com Allow",
                        new Caso(put(PESSOAS).with(jwt()), 405, "METODO_NAO_PERMITIDO", "Método não permitido", NADA,
                                header().exists("Allow"))),
                argumentSet("Accept não atendido → 406",
                        new Caso(get(PESSOAS).with(jwt()).accept(MediaType.APPLICATION_XML), 406, "NAO_ACEITAVEL",
                                "Formato não aceitável",
                                // o handler roda antes da negociação
                                t -> when(t.pessoas.listarPagina(1, 20)).thenReturn(new Pagina<>(List.of(), 1, 20, 0)))),
                argumentSet("Content-Type inválido → 415",
                        new Caso(post(PESSOAS).with(jwt()).contentType(MediaType.TEXT_PLAIN).content("x"), 415,
                                "TIPO_DE_MIDIA_NAO_SUPORTADO", "Tipo de conteúdo não suportado", NADA)),
                argumentSet("JSON malformado → 400",
                        new Caso(postJson("{\"documento\":"), 400, "CORPO_INVALIDO", "Corpo da requisição inválido", NADA)),
                argumentSet("CPF inválido no caminho → 400",
                        new Caso(get(PESSOAS + "/abc").with(jwt()), 400, "DOCUMENTO_INVALIDO", "Documento inválido", NADA)),
                argumentSet("CPF inválido no corpo → 400",
                        new Caso(postJson(JSON_PESSOA.formatted("11111111111", "Maria", "m@x.com")), 400,
                                "DOCUMENTO_INVALIDO", "Documento inválido", NADA)),
                argumentSet("validação do corpo → 400, com mapa de erros",
                        new Caso(postJson(JSON_PESSOA.formatted(CPF, " ", "x")), 400, "VALIDACAO_FALHOU",
                                "Validação falhou", NADA,
                                jsonPath("$.erros.nome").exists(), jsonPath("$.erros.email").exists())),
                argumentSet("parâmetro inválido → 400, com mapa de erros",
                        new Caso(get(PESSOAS).param("tamanho", "abc").with(jwt()), 400, "PARAMETRO_INVALIDO",
                                "Parâmetro inválido", NADA, jsonPath("$.erros.tamanho").isNotEmpty())),
                argumentSet("documento já cadastrado → 409",
                        new Caso(postJson(JSON_PESSOA.formatted(CPF, "A", "a@b.co")), 409, "DOCUMENTO_JA_CADASTRADO",
                                "Documento já cadastrado",
                                t -> when(t.pessoas.registrar(any())).thenThrow(new DocumentoJaCadastrado(CPF)))),
                argumentSet("pessoa inexistente → 404",
                        new Caso(get(PESSOAS + "/" + CPF).with(jwt()), 404, "PESSOA_NAO_ENCONTRADA", "Não encontrado",
                                t -> when(t.pessoas.buscar(Documento.de(CPF))).thenThrow(new PessoaNaoEncontrada(CPF)))),
                argumentSet("sem previsão → 404",
                        new Caso(get(PESSOAS + "/" + CPF + "/nacionalidade").with(jwt()), 404,
                                "PREVISAO_NAO_ENCONTRADA", "Não encontrado",
                                t -> when(t.nacionalidade.buscar(Documento.de(CPF))).thenThrow(new PrevisaoNaoEncontrada()))),
                argumentSet("serviço externo fora do ar → 502",
                        new Caso(get(PESSOAS + "/" + CPF + "/nacionalidade").with(jwt()), 502,
                                "NACIONALIDADE_INDISPONIVEL", "Serviço externo indisponível",
                                t -> when(t.nacionalidade.buscar(Documento.de(CPF)))
                                        .thenThrow(new NacionalidadeIndisponivel(new RuntimeException("fora"))))),
                argumentSet("erro inesperado → 500 genérico, sem vazar detalhe",
                        new Caso(get("/teste/explode").with(jwt()), 500, "ERRO_INTERNO", "Erro interno", NADA,
                                jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde"),
                                content().string(not(containsString("segredo-interno-123"))),
                                content().string(not(containsString("IllegalStateException"))))),
                argumentSet("status do corpo acompanha o do framework fora do catálogo → 413",
                        new Caso(get("/teste/grande").with(jwt()), 413, "REQUISICAO_INVALIDA", "Requisição inválida", NADA)));
    }

    private void problema(ResultActions r, int status, String codigo, String titulo) throws Exception {
        r.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.type").value("urn:testetecnico:cadastro:problema:" + codigo.toLowerCase().replace('_', '-')))
                .andExpect(jsonPath("$.title").value(titulo))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty())
                .andExpect(content().string(not(containsString("No static resource"))))
                .andExpect(content().string(not(containsString("Failed to read request"))))
                .andExpect(content().string(not(containsString("is not supported"))));
    }

    @ParameterizedTest
    @MethodSource("casos")
    void todoErroSaiComoProblemJsonDoCatalogo(Caso caso) throws Exception {
        caso.preparo().accept(this);

        var resposta = mvc.perform(caso.requisicao());

        problema(resposta, caso.status(), caso.codigo(), caso.titulo());
        for (var extra : caso.extras()) {
            resposta.andExpect(extra);
        }
    }

    @Test
    void handlerDeAcessoNegadoDoSecurityEscreveProblemJson() throws Exception {
        var req = new MockHttpServletRequest("GET", "/qualquer");
        var res = new MockHttpServletResponse();

        acessoNegado.handle(req, res, new AccessDeniedException("x"));

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentType()).contains("problem+json");
        assertThat(res.getContentAsString()).contains("\"codigo\":\"ACESSO_NEGADO\"");
    }

    @Test
    void erro5xxDoFrameworkEhLogadoEVemGenericoSemVazarDetalhe(CapturedOutput output) throws Exception {
        var resposta = mvc.perform(get("/teste/nao-serializa").with(jwt()));

        problema(resposta, 500, "ERRO_INTERNO", "Erro interno");
        resposta.andExpect(content().string(not(containsString("falha-de-serializacao-xyz"))));
        assertThat(output).contains("Erro inesperado do framework");
    }
}
