package br.com.testetecnico.cadastro.model;

import br.com.testetecnico.cadastro.exception.DocumentoInvalido;

/** CPF normalizado: exatamente 11 dígitos, não repetidos, com dígitos verificadores corretos. */
public record Documento(String valor) {

    public Documento {
        if (valor == null || !valor.matches("\\d{11}") || valor.chars().distinct().count() == 1
                || !digitosVerificadoresConferem(valor)) {
            throw new DocumentoInvalido();
        }
    }

    public static Documento de(String bruto) {
        if (bruto == null) {
            throw new DocumentoInvalido();
        }
        return new Documento(bruto.strip().replace(".", "").replace("-", ""));
    }

    private static boolean digitosVerificadoresConferem(String cpf) {
        return digito(cpf, 9) == cpf.charAt(9) - '0' && digito(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int digito(String cpf, int quantos) {
        int soma = 0;
        for (int i = 0; i < quantos; i++) {
            soma += (cpf.charAt(i) - '0') * (quantos + 1 - i);
        }
        return (soma * 10 % 11) % 10;
    }
}
