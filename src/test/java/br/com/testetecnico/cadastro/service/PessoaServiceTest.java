package br.com.testetecnico.cadastro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.testetecnico.cadastro.exception.DocumentoJaCadastrado;
import br.com.testetecnico.cadastro.exception.PessoaNaoEncontrada;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import br.com.testetecnico.cadastro.repository.PessoaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    private static final Sort ORDEM = Sort.by("nome", "sobrenome", "documento");

    @Mock
    PessoaRepository repositorio;
    @InjectMocks
    PessoaService servico;

    private final Documento doc = Documento.de("52998224725");
    private final Pessoa maria = new Pessoa(doc, "Maria", "Silva", "maria@x.com");

    @Test
    void recusaDocumentoJaCadastradoSemTentarGravar() {
        when(repositorio.existsByDocumento("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> servico.registrar(maria)).isInstanceOf(DocumentoJaCadastrado.class);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void corridaNaChaveViraDocumentoJaCadastrado() {
        when(repositorio.existsByDocumento("52998224725")).thenReturn(false);
        when(repositorio.saveAndFlush(maria)).thenThrow(new DataIntegrityViolationException("pk"));

        assertThatThrownBy(() -> servico.registrar(maria)).isInstanceOf(DocumentoJaCadastrado.class);
    }

    @Test
    void listarPaginaConverteParaIndiceZeroEMontaOsTotais() {
        var pedido = PageRequest.of(2, 2, ORDEM);
        when(repositorio.findAll(pedido)).thenReturn(new PageImpl<>(List.of(maria), pedido, 5));

        var pagina = servico.listarPagina(3, 2);

        assertThat(pagina.itens()).containsExactly(maria);
        assertThat(pagina.pagina()).isEqualTo(3);
        assertThat(pagina.tamanho()).isEqualTo(2);
        assertThat(pagina.totalItens()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }
}
