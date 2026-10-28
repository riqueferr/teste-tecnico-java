package br.com.testetecnico.cadastro.service;

import br.com.testetecnico.cadastro.exception.DocumentoJaCadastrado;
import br.com.testetecnico.cadastro.exception.PessoaNaoEncontrada;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import br.com.testetecnico.cadastro.repository.PessoaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private static final Sort ORDEM = Sort.by("nome", "sobrenome", "documento");

    private final PessoaRepository repositorio;

    public Pessoa registrar(Pessoa pessoa) {
        if (repositorio.existsByDocumento(pessoa.documento())) {
            throw new DocumentoJaCadastrado(pessoa.documento());
        }
        try {
            return repositorio.saveAndFlush(pessoa);
        } catch (DataIntegrityViolationException e) {
            // corrida: outro pedido gravou o mesmo documento entre a verificação e o INSERT
            throw new DocumentoJaCadastrado(pessoa.documento());
        }
    }

    public Pagina<Pessoa> listarPagina(int pagina, int tamanho) {
        // O Spring Data lança exceção (vira 500) se o offset passa de Integer.MAX_VALUE; essa página está além do fim.
        if ((long) (pagina - 1) * tamanho > Integer.MAX_VALUE) {
            return new Pagina<>(List.of(), pagina, tamanho, repositorio.count());
        }
        Page<Pessoa> resultado = repositorio.findAll(PageRequest.of(pagina - 1, tamanho, ORDEM));
        return new Pagina<>(resultado.getContent(), pagina, tamanho, resultado.getTotalElements());
    }

    public Pessoa buscar(Documento documento) {
        return repositorio.findByDocumento(documento.valor())
                .orElseThrow(() -> new PessoaNaoEncontrada(documento.valor()));
    }

    @Transactional
    public void excluir(Documento documento) {
        if (repositorio.excluirPorDocumento(documento.valor()) == 0) {
            throw new PessoaNaoEncontrada(documento.valor());
        }
    }
}
