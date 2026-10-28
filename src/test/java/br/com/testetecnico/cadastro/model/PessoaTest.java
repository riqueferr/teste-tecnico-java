package br.com.testetecnico.cadastro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class PessoaTest {

    @Test
    void primeiroNomeIgnoraEspacosEExcesso() {
        var pessoa = new Pessoa(Documento.de("52998224725"), "  Maria  da Silva ", "Souza", "m@x.com");
        assertThat(pessoa.primeiroNome()).isEqualTo("Maria");
    }

    @Test
    void geraIdUuidEDataDeCriacaoEmBrasilia() {
        var pessoa = new Pessoa(Documento.de("529.982.247-25"), "Maria", "Silva", "m@x.com");
        assertThat(pessoa.id()).isNotNull();
        assertThat(pessoa.criadoEm()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(Pessoa.FUSO).hasToString("-03:00");
    }

    @Test
    void toStringIdentificaAPessoaSemVazarOEmail() {
        var pessoa = new Pessoa(Documento.de("52998224725"), "Maria", "Silva", "segredo@x.com");
        assertThat(pessoa.toString()).contains("52998224725", "Maria", "Silva").doesNotContain("segredo@x.com");
    }
}
