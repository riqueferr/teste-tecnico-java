package br.com.testetecnico.cadastro.repository;

import br.com.testetecnico.cadastro.model.Pessoa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

    boolean existsByDocumento(String documento);

    Optional<Pessoa> findByDocumento(String documento);

    /** Exclusão atômica (um único DELETE); devolve quantas linhas saíram: 0 ou 1. Exige transação do chamador. */
    @Modifying
    @Query("delete from Pessoa p where p.documento = :documento")
    int excluirPorDocumento(@Param("documento") String documento);
}
