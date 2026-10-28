package br.com.testetecnico.cadastro.client;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class NationalizeConfig {

    @Bean
    NacionalidadeClient nacionalidadeClient(RestClient.Builder builder, NationalizeProperties propriedades) {
        var fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(propriedades.timeout()).build());
        fabrica.setReadTimeout(propriedades.timeout());
        return new NationalizeClient(builder.baseUrl(propriedades.baseUrl()).requestFactory(fabrica).build());
    }
}
