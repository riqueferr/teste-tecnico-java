package br.com.testetecnico.cadastro.controller.v1;

import br.com.testetecnico.cadastro.dto.NacionalidadeResponse;
import br.com.testetecnico.cadastro.dto.PaginaResponse;
import br.com.testetecnico.cadastro.dto.PessoaResponse;
import br.com.testetecnico.cadastro.dto.RegistrarPessoaRequest;
import br.com.testetecnico.cadastro.model.Documento;
import br.com.testetecnico.cadastro.model.Pessoa;
import br.com.testetecnico.cadastro.service.NacionalidadeService;
import br.com.testetecnico.cadastro.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pessoas")
@Tag(name = "Pessoas", description = "Cadastro de pessoas por CPF e previsão de nacionalidade")
@RequiredArgsConstructor
class PessoaV1Controller {

    private final PessoaService pessoas;
    private final NacionalidadeService nacionalidade;

    @Operation(summary = "Cadastra uma pessoa", operationId = "criarPessoa",
            description = "Registra a pessoa pelo CPF (aceita máscara; a chave guarda só dígitos).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pessoa criada",
                    headers = @Header(name = "Location", description = "URL da pessoa criada", schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = PessoaResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento já cadastrado",
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA)))})
    @PostMapping
    ResponseEntity<PessoaResponse> criar(@Valid @RequestBody RegistrarPessoaRequest req) {
        Pessoa pessoa = pessoas.registrar(req.paraEntidade());
        return ResponseEntity.created(URI.create("/api/v1/pessoas/" + pessoa.documento()))
                .body(PessoaResponse.de(pessoa));
    }

    @Operation(summary = "Lista pessoas (paginado)", operationId = "listarPessoas",
            description = "Ordenadas por nome, sobrenome e documento. `pagina` começa em 1; `tamanho` de 1 a 100.")
    @GetMapping
    PaginaResponse<PessoaResponse> listar(
            @RequestParam(defaultValue = "1") @Min(1) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return PaginaResponse.de(pessoas.listarPagina(pagina, tamanho), PessoaResponse::de);
    }

    @Operation(summary = "Busca uma pessoa pelo CPF", operationId = "buscarPessoa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pessoa encontrada"),
            @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA)))})
    @GetMapping("/{documento}")
    PessoaResponse buscar(@PathVariable String documento) {
        return PessoaResponse.de(pessoas.buscar(Documento.de(documento)));
    }

    @Operation(summary = "Exclui uma pessoa pelo CPF", operationId = "excluirPessoa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pessoa excluída"),
            @ApiResponse(responseCode = "404", description = "Pessoa não encontrada",
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA)))})
    @DeleteMapping("/{documento}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void excluir(@PathVariable String documento) {
        pessoas.excluir(Documento.de(documento));
    }

    @Operation(summary = "Consulta a nacionalidade provável da pessoa", operationId = "nacionalidadeDaPessoa",
            description = "Usa o primeiro nome com a API pública nationalize.io (limite diário gratuito).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nacionalidade provável"),
            @ApiResponse(responseCode = "404", description = "Pessoa ou previsão não encontrada",
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA))),
            @ApiResponse(responseCode = "502", description = "Serviço de previsão indisponível",
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA)))})
    @GetMapping("/{documento}/nacionalidade")
    NacionalidadeResponse nacionalidade(@PathVariable String documento) {
        return NacionalidadeResponse.de(nacionalidade.buscar(Documento.de(documento)));
    }
}
