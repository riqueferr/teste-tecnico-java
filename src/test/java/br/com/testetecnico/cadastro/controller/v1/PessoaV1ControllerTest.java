package br.com.testetecnico.cadastro.controller.v1;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.testetecnico.cadastro.ContextoDeTeste;
import br.com.testetecnico.cadastro.client.Previsao;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import br.com.testetecnico.cadastro.security.SecurityConfig;
import br.com.testetecnico.cadastro.service.NacionalidadeService;
import br.com.testetecnico.cadastro.service.Pagina;
import br.com.testetecnico.cadastro.service.PessoaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PessoaV1Controller.class)
@Import(SecurityConfig.class)
class PessoaV1ControllerTest extends ContextoDeTeste {

    private static final String CPF = "52998224725";

    @Autowired
    MockMvc mvc;
    @MockitoBean
    PessoaService pessoas;
    @MockitoBean
    NacionalidadeService nacionalidade;

    private final Documento doc = Documento.de(CPF);
    private final Pessoa maria = new Pessoa(doc, "Maria", "Silva", "maria@x.com");

    @Test
    void criaRetorna201ComLocationDaV1NormalizandoDocumentoENomes() throws Exception {
        when(pessoas.registrar(any())).thenReturn(maria);

        mvc.perform(post("/api/v1/pessoas").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("529.982.247-25", "  Maria ", " Silva  ", "maria@x.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pessoas/" + CPF))
                .andExpect(jsonPath("$.id").value(maria.id().toString()))
                .andExpect(jsonPath("$.documento").value(CPF))
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.criadoEm", endsWith("-03:00")));
        verify(pessoas).registrar(maria);
    }

    @Test
    void listaUsaPagina1Tamanho20SemParametrosERepassaOsPedidosNoEnvelope() throws Exception {
        when(pessoas.listarPagina(1, 20)).thenReturn(new Pagina<>(List.of(maria), 1, 20, 1));
        when(pessoas.listarPagina(2, 5)).thenReturn(new Pagina<>(List.of(), 2, 5, 6));

        mvc.perform(get("/api/v1/pessoas").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].documento").value(CPF))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
        mvc.perform(get("/api/v1/pessoas").param("pagina", "2").param("tamanho", "5").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.totalItens").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @ParameterizedTest
    @CsvSource({
            "pagina,0,deve ser maior que ou igual à 1",
            "pagina,2147483648,deve ser um número inteiro",
            "tamanho,0,deve ser maior que ou igual à 1",
            "tamanho,101,deve ser menor que ou igual à 100",
            "tamanho,abc,deve ser um número inteiro"})
    void paginacaoInvalidaRetorna400ComMensagemEmPortuguesNoParametro(String parametro, String valor, String mensagem)
            throws Exception {
        mvc.perform(get("/api/v1/pessoas").param(parametro, valor).with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"))
                .andExpect(jsonPath("$.erros." + parametro).value(mensagem));
        verifyNoInteractions(pessoas);
    }

    @Test
    void buscaPorDocumentoAceitandoMascara() throws Exception {
        when(pessoas.buscar(doc)).thenReturn(maria);

        mvc.perform(get("/api/v1/pessoas/529.982.247-25").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sobrenome").value("Silva"));
    }

    @Test
    void excluiRetorna204() throws Exception {
        mvc.perform(delete("/api/v1/pessoas/" + CPF).with(jwt())).andExpect(status().isNoContent());
        verify(pessoas).excluir(doc);
    }

    @Test
    void nacionalidadeRetornaIsoENomeDoPaisEmPortugues() throws Exception {
        when(nacionalidade.buscar(doc)).thenReturn(new NacionalidadeService.Resultado(maria, new Previsao("BR", 0.87)));

        mvc.perform(get("/api/v1/pessoas/" + CPF + "/nacionalidade").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paisIso").value("BR"))
                .andExpect(jsonPath("$.nomePais").value("Brasil"))
                .andExpect(jsonPath("$.probabilidade").value(0.87));
    }

    @Test
    void mensagensDeValidacaoDoCorpoVemEmPortuguesMesmoSemAcceptLanguage() throws Exception {
        mvc.perform(post("/api/v1/pessoas").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(CPF, " ", "Silva", "nao-e-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").value("não deve estar em branco"))
                .andExpect(jsonPath("$.erros.email").value("deve ser um endereço de e-mail bem formado"));
    }

    private static String corpo(String documento, String nome, String sobrenome, String email) {
        return "{\"documento\":\"" + documento + "\",\"nome\":\"" + nome + "\",\"sobrenome\":\"" + sobrenome
                + "\",\"email\":\"" + email + "\"}";
    }

    /** E-mail válido com exatamente {@code n} caracteres: "a@" + domínio em rótulos de até 50 letras. */
    private static String emailComTamanho(int n) {
        var dominio = new StringBuilder();
        for (int i = 0; i < n - 2; i++) {
            dominio.append(i % 50 == 49 && i < n - 5 ? '.' : 'b');
        }
        return "a@" + dominio;
    }

    @Test
    void camposNoTamanhoMaximoSaoAceitos() throws Exception {
        when(pessoas.registrar(any())).thenReturn(maria);

        mvc.perform(post("/api/v1/pessoas").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(CPF, "n".repeat(100), "s".repeat(100), emailComTamanho(255))))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource({"nome,101", "sobrenome,101", "email,256"})
    void camposAcimaDoTamanhoMaximoRetornam400ComErroNoCampo(String campo, int tamanho) throws Exception {
        String nome = campo.equals("nome") ? "n".repeat(tamanho) : "Maria";
        String sobrenome = campo.equals("sobrenome") ? "s".repeat(tamanho) : "Silva";
        String email = campo.equals("email") ? emailComTamanho(tamanho) : "maria@x.com";

        mvc.perform(post("/api/v1/pessoas").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(CPF, nome, sobrenome, email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros." + campo).isNotEmpty());
        verifyNoInteractions(pessoas);
    }
}
