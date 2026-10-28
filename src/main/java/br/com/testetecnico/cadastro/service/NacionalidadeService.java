package br.com.testetecnico.cadastro.service;

import br.com.testetecnico.cadastro.client.NacionalidadeClient;
import br.com.testetecnico.cadastro.client.Previsao;
import br.com.testetecnico.cadastro.exception.PrevisaoNaoEncontrada;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NacionalidadeService {

    public record Resultado(Pessoa pessoa, Previsao previsao) {
    }

    private final PessoaService pessoas;
    private final NacionalidadeClient cliente;

    public Resultado buscar(Documento documento) {
        Pessoa pessoa = pessoas.buscar(documento);
        Previsao previsao = cliente.prever(pessoa.primeiroNome()).orElseThrow(PrevisaoNaoEncontrada::new);
        return new Resultado(pessoa, previsao);
    }
}
