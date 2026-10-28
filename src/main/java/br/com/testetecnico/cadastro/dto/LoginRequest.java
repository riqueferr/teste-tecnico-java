package br.com.testetecnico.cadastro.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String usuario, @NotBlank String senha) {
}
