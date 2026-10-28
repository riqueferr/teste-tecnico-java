package br.com.testetecnico.cadastro.dto;

import br.com.testetecnico.cadastro.service.NacionalidadeService;
import java.util.Locale;

public record NacionalidadeResponse(String documento, String nome, String paisIso, String nomePais,
                                    double probabilidade) {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public static NacionalidadeResponse de(NacionalidadeService.Resultado r) {
        String iso = r.previsao().paisIso();
        return new NacionalidadeResponse(
                r.pessoa().documento(),
                r.pessoa().nome(),
                iso,
                Locale.of("", iso).getDisplayCountry(PT_BR),
                r.previsao().probabilidade());
    }
}
