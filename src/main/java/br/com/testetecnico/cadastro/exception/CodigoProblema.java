package br.com.testetecnico.cadastro.exception;

import java.net.URI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CodigoProblema {

    VALIDACAO_FALHOU(400, "Validação falhou", "Um ou mais campos são inválidos"),
    PARAMETRO_INVALIDO(400, "Parâmetro inválido", "Um ou mais parâmetros são inválidos"),
    DOCUMENTO_INVALIDO(400, "Documento inválido", null),
    CORPO_INVALIDO(400, "Corpo da requisição inválido", "O corpo da requisição está ausente ou não é um JSON válido"),
    REQUISICAO_INVALIDA(400, "Requisição inválida", "A requisição não pôde ser processada"),
    NAO_AUTENTICADO(401, "Não autenticado", "Credenciais ausentes, inválidas ou expiradas"),
    ACESSO_NEGADO(403, "Acesso negado", "Você não tem permissão para acessar este recurso"),
    PESSOA_NAO_ENCONTRADA(404, "Não encontrado", null),
    PREVISAO_NAO_ENCONTRADA(404, "Não encontrado", null),
    RECURSO_NAO_ENCONTRADO(404, "Não encontrado", "A rota solicitada não existe"),
    METODO_NAO_PERMITIDO(405, "Método não permitido", "O método HTTP não é aceito para este recurso"),
    NAO_ACEITAVEL(406, "Formato não aceitável", "A API só responde em application/json"),
    DOCUMENTO_JA_CADASTRADO(409, "Documento já cadastrado", null),
    TIPO_DE_MIDIA_NAO_SUPORTADO(415, "Tipo de conteúdo não suportado", "Envie o corpo como application/json"),
    MUITAS_TENTATIVAS(429, "Muitas tentativas", "Aguarde antes de tentar novamente"),
    ERRO_INTERNO(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente mais tarde"),
    NACIONALIDADE_INDISPONIVEL(502, "Serviço externo indisponível", null);

    private final int status;
    private final String titulo;
    private final String detalhePadrao;

    public URI tipo() {
        return URI.create("urn:testetecnico:cadastro:problema:" + name().toLowerCase().replace('_', '-'));
    }
}
