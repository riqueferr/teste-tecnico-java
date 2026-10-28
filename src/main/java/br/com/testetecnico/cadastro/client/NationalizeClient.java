package br.com.testetecnico.cadastro.client;

import br.com.testetecnico.cadastro.exception.NacionalidadeIndisponivel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RequiredArgsConstructor
class NationalizeClient implements NacionalidadeClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Resposta(List<Pais> country) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Pais(@JsonProperty("country_id") String countryId, double probability) {
    }

    private final RestClient cliente;

    @Override
    public Optional<Previsao> prever(String primeiroNome) {
        Resposta resposta;
        try {
            resposta = cliente.get().uri("/?name={nome}", primeiroNome).retrieve().body(Resposta.class);
        } catch (RestClientException e) {
            // cobre 4xx (incl. 429 de limite diário), 5xx, timeout e falha de rede
            throw new NacionalidadeIndisponivel(e);
        }
        if (resposta == null || resposta.country() == null) {
            return Optional.empty();
        }
        return resposta.country().stream()
                .max(Comparator.comparingDouble(Pais::probability))
                .map(p -> new Previsao(p.countryId(), p.probability()));
    }
}
