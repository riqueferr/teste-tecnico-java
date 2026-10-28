package br.com.testetecnico.cadastro.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.nationalize")
record NationalizeProperties(String baseUrl, Duration timeout) {
}
