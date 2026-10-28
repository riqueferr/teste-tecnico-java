package br.com.testetecnico.cadastro.exception;

public class DocumentoJaCadastrado extends RuntimeException {

    public DocumentoJaCadastrado(String documento) {
        super("Já existe pessoa cadastrada com o documento " + documento);
    }
}
