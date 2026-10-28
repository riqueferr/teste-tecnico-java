package br.com.testetecnico.cadastro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.testetecnico.cadastro.client.NacionalidadeClient;
import br.com.testetecnico.cadastro.client.Previsao;
import br.com.testetecnico.cadastro.exception.PrevisaoNaoEncontrada;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NacionalidadeServiceTest {

    @Mock
    PessoaService pessoas;
    @Mock
    NacionalidadeClient cliente;
    @InjectMocks
    NacionalidadeService servico;

    private final Documento doc = Documento.de("52998224725");

    @Test
    void consultaPeloPrimeiroNomeERetornaAPrevisao() {
        var maria = new Pessoa(doc, "Maria Clara", "Silva", "maria@x.com");
        when(pessoas.buscar(doc)).thenReturn(maria);
        when(cliente.prever("Maria")).thenReturn(Optional.of(new Previsao("BR", 0.8)));

        var resultado = servico.buscar(doc);

        assertThat(resultado.pessoa()).isEqualTo(maria);
        assertThat(resultado.previsao()).isEqualTo(new Previsao("BR", 0.8));
        verify(cliente).prever("Maria");
    }

    @Test
    void semPrevisaoLancaPrevisaoNaoEncontrada() {
        when(pessoas.buscar(doc)).thenReturn(new Pessoa(doc, "Xyzzy", "Silva", "x@x.com"));
        when(cliente.prever("Xyzzy")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.buscar(doc)).isInstanceOf(PrevisaoNaoEncontrada.class);
    }
}
