package br.com.testetecnico.cadastro.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaTest {

    @Test
    void totalDePaginasArredondaParaCimaEEZeroSemItens() {
        assertThat(new Pagina<String>(List.of(), 1, 20, 41).totalPaginas()).isEqualTo(3);
        assertThat(new Pagina<String>(List.of(), 1, 20, 40).totalPaginas()).isEqualTo(2);
        assertThat(new Pagina<String>(List.of(), 1, 20, 1).totalPaginas()).isEqualTo(1);
        assertThat(new Pagina<String>(List.of(), 1, 20, 0).totalPaginas()).isZero();
    }
}
