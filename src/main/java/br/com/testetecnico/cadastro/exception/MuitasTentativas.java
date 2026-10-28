package br.com.testetecnico.cadastro.exception;

import lombok.Getter;

public class MuitasTentativas extends RuntimeException {

    @Getter
    private final long retryAfterSegundos;

    public MuitasTentativas(long retryAfterSegundos) {
        super("Muitas tentativas de login. Tente novamente em " + retryAfterSegundos + " segundos");
        this.retryAfterSegundos = retryAfterSegundos;
    }
}
