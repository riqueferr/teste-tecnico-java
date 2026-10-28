package br.com.testetecnico.cadastro.controller.v1;

import br.com.testetecnico.cadastro.dto.LoginRequest;
import br.com.testetecnico.cadastro.dto.TokenResponse;
import br.com.testetecnico.cadastro.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
@RequiredArgsConstructor
class AuthV1Controller {

    private final AutenticacaoService servico;

    @Operation(summary = "Autentica e devolve o token JWT", operationId = "login")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido"),
            @ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos",
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA))),
            @ApiResponse(responseCode = "429", description = "Muitas tentativas de login (veja Retry-After)",
                    headers = @Header(name = "Retry-After", description = "Segundos até poder tentar de novo", schema = @Schema(type = "integer")),
                    content = @Content(mediaType = Contrato.PROBLEMA, schema = @Schema(ref = Contrato.REF_PROBLEMA)))})
    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest req) {
        AutenticacaoService.Token token = servico.autenticar(req.usuario(), req.senha());
        return new TokenResponse(token.token(), token.expiraEm());
    }
}
