package br.com.testetecnico.cadastro.exception;

public class NacionalidadeIndisponivel extends RuntimeException {

    public NacionalidadeIndisponivel(Throwable causa) {
        super("Serviço de previsão de nacionalidade indisponível", causa);
    }
}
