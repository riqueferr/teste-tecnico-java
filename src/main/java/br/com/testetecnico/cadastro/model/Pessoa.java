package br.com.testetecnico.cadastro.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pessoa")
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pessoa {

    public static final ZoneOffset FUSO = ZoneOffset.ofHours(-3); // horário de Brasília

    // igualdade pelos dados informados: id e criadoEm são gerados na criação
    @Id
    @EqualsAndHashCode.Exclude
    private UUID id;

    @Column(nullable = false, unique = true, length = 11)
    private String documento;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100)
    private String sobrenome;

    @Column(nullable = false)
    private String email;

    @EqualsAndHashCode.Exclude
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    public Pessoa(Documento documento, String nome, String sobrenome, String email) {
        this.id = UUID.randomUUID();
        this.criadoEm = Instant.now().truncatedTo(ChronoUnit.MICROS); // precisão do timestamptz
        this.documento = documento.valor();
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
    }

    public String primeiroNome() {
        return nome.strip().split("\\s+")[0];
    }

    @Override
    public String toString() {
        return "Pessoa[documento=" + documento + ", nome=" + nome + " " + sobrenome + "]";
    }
}
