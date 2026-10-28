package br.com.testetecnico.cadastro.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import br.com.testetecnico.cadastro.exception.NacionalidadeIndisponivel;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

class NationalizeClientTest {

    private MockRestServiceServer servidor;
    private NationalizeClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.nationalize.io");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new NationalizeClient(builder.build());
    }

    @Test
    void escolheOPaisDeMaiorProbabilidadeIgnorandoCamposExtras() {
        servidor.expect(requestTo("https://api.nationalize.io/?name=Nathaniel")).andRespond(withSuccess("""
                {"count":1234,"name":"Nathaniel","country":[
                  {"country_id":"US","probability":0.21},
                  {"country_id":"NZ","probability":0.43},
                  {"country_id":"GB","probability":0.12}]}
                """, MediaType.APPLICATION_JSON));

        assertThat(cliente.prever("Nathaniel")).contains(new Previsao("NZ", 0.43));
    }

    @Test
    void listaVaziaViraOptionalVazio() {
        servidor.expect(requestTo("https://api.nationalize.io/?name=Xyzzy")).andRespond(withSuccess("""
                {"count":0,"name":"Xyzzy","country":[]}
                """, MediaType.APPLICATION_JSON));

        assertThat(cliente.prever("Xyzzy")).isEmpty();
    }

    static Stream<Arguments> falhas() {
        return Stream.of(
                argumentSet("erro 5xx", withServerError()),
                argumentSet("limite excedido (429)", withTooManyRequests()),
                argumentSet("falha de rede ou timeout", (ResponseCreator) request -> {
                    throw new IOException("timeout");
                }));
    }

    @ParameterizedTest
    @MethodSource("falhas")
    void qualquerFalhaDoServicoViraNacionalidadeIndisponivel(ResponseCreator resposta) {
        servidor.expect(requestTo("https://api.nationalize.io/?name=Maria")).andRespond(resposta);

        assertThatThrownBy(() -> cliente.prever("Maria")).isInstanceOf(NacionalidadeIndisponivel.class);
    }
}
