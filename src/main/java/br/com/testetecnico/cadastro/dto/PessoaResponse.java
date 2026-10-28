package br.com.testetecnico.cadastro.dto;

import br.com.testetecnico.cadastro.model.Pessoa;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PessoaResponse(UUID id, String documento, String nome, String sobrenome, String email,
                             OffsetDateTime criadoEm) {

    public static PessoaResponse de(Pessoa p) {
        return new PessoaResponse(p.id(), p.documento(), p.nome(), p.sobrenome(), p.email(),
                p.criadoEm().atOffset(Pessoa.FUSO));
    }
}
