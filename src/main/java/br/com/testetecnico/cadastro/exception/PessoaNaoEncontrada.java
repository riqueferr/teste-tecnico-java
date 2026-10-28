package br.com.testetecnico.cadastro.exception;

public class PessoaNaoEncontrada extends RuntimeException {

    public PessoaNaoEncontrada(String documento) {
        super("Pessoa não encontrada para o documento " + documento);
    }
}
