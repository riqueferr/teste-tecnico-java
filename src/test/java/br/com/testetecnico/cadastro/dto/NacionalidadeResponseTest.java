package br.com.testetecnico.cadastro.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.testetecnico.cadastro.client.Previsao;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import br.com.testetecnico.cadastro.service.NacionalidadeService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NacionalidadeResponseTest {

    private final Pessoa maria = new Pessoa(Documento.de("52998224725"), "Maria", "Silva", "m@x.com");

    @ParameterizedTest
    @CsvSource({"BR,Brasil", "XX,XX"}) // XX: sem nome no locale, devolve o próprio código
    void nomeDoPaisEmPortuguesComFallbackParaOCodigo(String iso, String esperado) {
        var resposta = NacionalidadeResponse.de(new NacionalidadeService.Resultado(maria, new Previsao(iso, 0.5)));

        assertThat(resposta.paisIso()).isEqualTo(iso);
        assertThat(resposta.nomePais()).isEqualTo(esperado);
    }
}
