package br.com.testetecnico.cadastro.service;

import java.util.List;

public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalItens) {

    public int totalPaginas() {
        return (int) ((totalItens + tamanho - 1) / tamanho);
    }
}
