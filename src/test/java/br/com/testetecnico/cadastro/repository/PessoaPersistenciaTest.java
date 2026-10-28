package br.com.testetecnico.cadastro.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.testetecnico.cadastro.PostgresBase;
import br.com.testetecnico.cadastro.exception.PessoaNaoEncontrada;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import br.com.testetecnico.cadastro.service.PessoaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PessoaPersistenciaTest extends PostgresBase {

    @Autowired
    PessoaRepository repositorio;
    @Autowired
    PessoaService servico;
    @Autowired
    JdbcTemplate jdbc;

    private final Documento doc = Documento.de("52998224725");
    private final Pessoa maria = new Pessoa(doc, "Maria", "Silva", "maria@x.com");

    @BeforeEach
    void limpar() {
        jdbc.update("delete from pessoa");
    }

    @Test
    void salvaEBuscaDevolvendoOMesmoConteudoEChaveDuplicadaViolaAUnicidadeSemSobrescrever() {
        repositorio.saveAndFlush(maria);
        var outra = new Pessoa(doc, "Outra", "Pessoa", "outra@x.com");

        assertThatThrownBy(() -> repositorio.saveAndFlush(outra)).isInstanceOf(DataIntegrityViolationException.class);
        var salva = repositorio.findByDocumento("52998224725");
        assertThat(salva).contains(maria);
        assertThat(salva.orElseThrow().id()).isEqualTo(maria.id());
        assertThat(salva.orElseThrow().criadoEm()).isEqualTo(maria.criadoEm());
        assertThat(repositorio.existsByDocumento("52998224725")).isTrue();
    }

    @Test
    void excluiEDepoisNaoEncontra() {
        servico.registrar(maria);

        servico.excluir(doc);

        assertThat(repositorio.existsByDocumento("52998224725")).isFalse();
        assertThatThrownBy(() -> servico.excluir(doc)).isInstanceOf(PessoaNaoEncontrada.class);
    }

    @Test
    void listaOrdenadaPorNomeSobrenomeEDocumentoPaginadaEVaziaAlemDoFim() {
        var anaCostaB = new Pessoa(Documento.de("11144477735"), "Ana", "Costa", "a2@x.com");
        var anaCostaA = new Pessoa(Documento.de("12345678909"), "Ana", "Costa", "a3@x.com");
        var anaSilva = new Pessoa(Documento.de("52998224725"), "Ana", "Silva", "a1@x.com");
        var bruno = new Pessoa(Documento.de("39053344705"), "Bruno", "Alves", "b@x.com");
        var carla = new Pessoa(Documento.de("70499962206"), "Carla", "Dias", "c@x.com");
        for (Pessoa p : new Pessoa[] {carla, anaSilva, anaCostaA, bruno, anaCostaB}) {
            servico.registrar(p);
        }

        assertThat(servico.listarPagina(1, 100).itens()).containsExactly(anaCostaB, anaCostaA, anaSilva, bruno, carla);

        var segunda = servico.listarPagina(2, 2);
        assertThat(segunda.itens()).containsExactly(anaSilva, bruno);
        assertThat(segunda.totalItens()).isEqualTo(5);
        assertThat(segunda.totalPaginas()).isEqualTo(3);

        var alemDoFim = servico.listarPagina(4, 2);
        assertThat(alemDoFim.itens()).isEmpty();
        assertThat(alemDoFim.totalItens()).isEqualTo(5);

        var alemDoInt = servico.listarPagina(Integer.MAX_VALUE, 100); // o offset estoura int: vazia, sem exceção
        assertThat(alemDoInt.itens()).isEmpty();
        assertThat(alemDoInt.totalItens()).isEqualTo(5);
    }
}
