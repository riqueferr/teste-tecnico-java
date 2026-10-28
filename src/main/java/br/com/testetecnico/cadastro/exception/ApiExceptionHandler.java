package br.com.testetecnico.cadastro.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
class ApiExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(DocumentoInvalido.class)
    ProblemDetail documentoInvalido(DocumentoInvalido e) {
        return problema(CodigoProblema.DOCUMENTO_INVALIDO, e.getMessage());
    }

    @ExceptionHandler(DocumentoJaCadastrado.class)
    ProblemDetail documentoJaCadastrado(DocumentoJaCadastrado e) {
        return problema(CodigoProblema.DOCUMENTO_JA_CADASTRADO, e.getMessage());
    }

    @ExceptionHandler(PessoaNaoEncontrada.class)
    ProblemDetail pessoaNaoEncontrada(PessoaNaoEncontrada e) {
        return problema(CodigoProblema.PESSOA_NAO_ENCONTRADA, e.getMessage());
    }

    @ExceptionHandler(PrevisaoNaoEncontrada.class)
    ProblemDetail previsaoNaoEncontrada(PrevisaoNaoEncontrada e) {
        return problema(CodigoProblema.PREVISAO_NAO_ENCONTRADA, e.getMessage());
    }

    @ExceptionHandler(NacionalidadeIndisponivel.class)
    ProblemDetail nacionalidadeIndisponivel(NacionalidadeIndisponivel e) {
        log.warn("Falha ao consultar a nationalize.io", e);
        return problema(CodigoProblema.NACIONALIDADE_INDISPONIVEL, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail naoAutenticado(AuthenticationException e) {
        return problema(CodigoProblema.NAO_AUTENTICADO, e instanceof BadCredentialsException ? e.getMessage() : null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado(AccessDeniedException e) {
        return problema(CodigoProblema.ACESSO_NEGADO, null);
    }

    @ExceptionHandler(MuitasTentativas.class)
    ResponseEntity<ProblemDetail> muitasTentativas(MuitasTentativas e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSegundos()))
                .body(problema(CodigoProblema.MUITAS_TENTATIVAS, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return problema(CodigoProblema.ERRO_INTERNO, null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(erro.getField(), erro.getDefaultMessage() == null ? "inválido" : erro.getDefaultMessage());
        }
        ProblemDetail problema = problema(CodigoProblema.VALIDACAO_FALHOU, null);
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (ParameterValidationResult resultado : ex.getParameterValidationResults()) {
            String parametro = resultado.getMethodParameter().getParameterName();
            for (MessageSourceResolvable erro : resultado.getResolvableErrors()) {
                erros.putIfAbsent(parametro, erro.getDefaultMessage() == null ? "inválido" : erro.getDefaultMessage());
            }
        }
        return parametroInvalido(ex, erros, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return parametroInvalido(ex, Map.of(parametro == null ? "parametro" : parametro, "deve ser um número inteiro"),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // o Spring chega aqui com body == null (a ProblemDetail só é montada no super), então null também é traduzido
        if (body == null || (body instanceof ProblemDetail pd
                && (pd.getProperties() == null || !pd.getProperties().containsKey("codigo")))) {
            if (statusCode.is5xxServerError()) {
                log.error("Erro inesperado do framework", ex);
            }
            ProblemDetail traduzido = problema(codigoDoFramework(ex, statusCode), null);
            traduzido.setStatus(statusCode.value());
            return super.handleExceptionInternal(ex, traduzido, headers, statusCode, request);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ResponseEntity<Object> parametroInvalido(
            Exception ex, Map<String, String> erros, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = problema(CodigoProblema.PARAMETRO_INVALIDO, null);
        problema.setProperty("erros", erros);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    private static CodigoProblema codigoDoFramework(Exception ex, HttpStatusCode status) {
        if (ex instanceof HttpMessageNotReadableException) {
            return CodigoProblema.CORPO_INVALIDO;
        }
        return switch (status.value()) {
            case 404 -> CodigoProblema.RECURSO_NAO_ENCONTRADO;
            case 405 -> CodigoProblema.METODO_NAO_PERMITIDO;
            case 406 -> CodigoProblema.NAO_ACEITAVEL;
            case 415 -> CodigoProblema.TIPO_DE_MIDIA_NAO_SUPORTADO;
            default -> status.is5xxServerError() ? CodigoProblema.ERRO_INTERNO : CodigoProblema.REQUISICAO_INVALIDA;
        };
    }

    private static ProblemDetail problema(CodigoProblema codigo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(codigo.status()), detalhe != null ? detalhe : codigo.detalhePadrao());
        problema.setType(codigo.tipo());
        problema.setTitle(codigo.titulo());
        problema.setProperty("codigo", codigo.name());
        return problema;
    }
}
