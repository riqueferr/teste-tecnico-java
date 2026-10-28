package br.com.testetecnico.cadastro.exception;

public class DocumentoInvalido extends RuntimeException {

    public DocumentoInvalido() {
        super("Documento inválido: informe um CPF válido com 11 dígitos");
    }
}
