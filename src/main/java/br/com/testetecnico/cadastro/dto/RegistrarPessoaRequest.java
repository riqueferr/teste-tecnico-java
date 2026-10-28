package br.com.testetecnico.cadastro.dto;

import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarPessoaRequest(
        @NotBlank String documento,
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 100) String sobrenome,
        @NotBlank @Email @Size(max = 255) String email) {

    public Pessoa paraEntidade() {
        return new Pessoa(Documento.de(documento), nome.strip(), sobrenome.strip(), email.strip());
    }
}
