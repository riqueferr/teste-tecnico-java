package br.com.testetecnico.cadastro.client;

import br.com.testetecnico.cadastro.exception.NacionalidadeIndisponivel;
import java.util.Optional;

public interface NacionalidadeClient {

    /** Vazio se não houver previsão; lança {@link NacionalidadeIndisponivel} se o serviço externo falhar. */
    Optional<Previsao> prever(String primeiroNome);
}
