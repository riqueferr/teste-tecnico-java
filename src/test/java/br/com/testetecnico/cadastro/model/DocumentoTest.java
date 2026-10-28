package br.com.testetecnico.cadastro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.testetecnico.cadastro.exception.DocumentoInvalido;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", " 529.982.247-25 ", "704.999.622-06"}) // o último: dígito verificador 0 (resto 10)
    void aceitaCpfValidoComOuSemMascaraNormalizandoParaDigitos(String bruto) {
        assertThat(Documento.de(bruto).valor()).isEqualTo(bruto.replaceAll("\\D", ""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "5299822472", "529982247250", "abc82224725", "52998224726", "52998224733", "11111111111", "00000000000"})
    void rejeitaCpfInvalido(String bruto) {
        assertThatThrownBy(() -> Documento.de(bruto)).isInstanceOf(DocumentoInvalido.class);
    }
}
