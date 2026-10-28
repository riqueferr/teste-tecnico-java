package br.com.testetecnico.cadastro.dto;

import br.com.testetecnico.cadastro.service.Pagina;
import java.util.List;
import java.util.function.Function;

public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {

    public static <S, T> PaginaResponse<T> de(Pagina<S> pagina, Function<S, T> conversor) {
        return new PaginaResponse<>(
                pagina.itens().stream().map(conversor).toList(),
                pagina.pagina(), pagina.tamanho(), pagina.totalItens(), pagina.totalPaginas());
    }
}
