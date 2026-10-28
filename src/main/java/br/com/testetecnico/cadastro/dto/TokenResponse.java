package br.com.testetecnico.cadastro.dto;

import java.time.Instant;

public record TokenResponse(String token, Instant expiraEm) {
}
