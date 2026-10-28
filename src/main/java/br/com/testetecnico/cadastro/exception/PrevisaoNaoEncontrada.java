package br.com.testetecnico.cadastro.exception;

public class PrevisaoNaoEncontrada extends RuntimeException {

    public PrevisaoNaoEncontrada() {
        super("Não há previsão de nacionalidade para este nome");
    }
}
