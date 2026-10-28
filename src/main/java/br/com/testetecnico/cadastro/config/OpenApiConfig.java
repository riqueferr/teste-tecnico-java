package br.com.testetecnico.cadastro.config;

import br.com.testetecnico.cadastro.exception.CodigoProblema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Arrays;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Cadastro de Pessoas", version = "1.0.0",
                description = "API de cadastro de pessoas por CPF com previsão de nacionalidade (`/api/v1`)."),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {

    private static final String PROBLEMA = "application/problem+json";

    @Bean
    GroupedOpenApi grupoV1() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .addOpenApiCustomizer(esquemaEErrosPadrao())
                .build();
    }

    private OpenApiCustomizer esquemaEErrosPadrao() {
        return openApi -> {
            openApi.getComponents().addSchemas("Problema", esquemaProblema());
            operacoes(openApi).forEach(op -> {
                boolean protegida = op.getSecurity() == null || !op.getSecurity().isEmpty();
                boolean temEntrada = op.getRequestBody() != null || (op.getParameters() != null && !op.getParameters().isEmpty());
                if (temEntrada) {
                    acrescentar(op, "400", "Requisição inválida (validação, CPF ou parâmetro)");
                }
                if (protegida) {
                    acrescentar(op, "401", "Não autenticado: token ausente, inválido ou expirado");
                }
                acrescentar(op, "500", "Erro interno inesperado");
            });
        };
    }

    private static Schema<?> esquemaProblema() {
        return new ObjectSchema()
                .description("Erro no formato problem+json (RFC 9457) com o código estável `codigo`")
                .addProperty("type", new StringSchema().example("urn:testetecnico:cadastro:problema:documento-invalido"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("codigo", new StringSchema()._enum(
                        Arrays.stream(CodigoProblema.values()).map(Enum::name).toList()))
                .addProperty("erros", new MapSchema().additionalProperties(new StringSchema())
                        .description("Mensagem por campo/parâmetro (só nos erros de validação)"));
    }

    private static void acrescentar(Operation op, String codigo, String descricao) {
        if (op.getResponses().containsKey(codigo)) {
            return;
        }
        op.getResponses().addApiResponse(codigo, new ApiResponse().description(descricao).content(
                new Content().addMediaType(PROBLEMA, new io.swagger.v3.oas.models.media.MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/Problema")))));
    }

    private static Stream<Operation> operacoes(OpenAPI openApi) {
        return openApi.getPaths().values().stream().flatMap(item -> item.readOperations().stream());
    }
}
