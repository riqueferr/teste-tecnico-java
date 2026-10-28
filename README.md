# Cadastro de Pessoas

API REST para cadastrar pessoas por CPF e consultar a provável nacionalidade do primeiro nome, usando a API pública [nationalize.io](https://nationalize.io). Inclui uma interface web simples servida pela própria aplicação.

## Stack

| | |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Banco | PostgreSQL 16, migrations com Flyway |
| Autenticação | JWT HS256 (Spring Security OAuth2 Resource Server), usuário único configurado por variável de ambiente |
| Cliente HTTP | `RestClient` (nationalize.io, timeout de 3 s) |
| Documentação da API | OpenAPI 3 / Swagger UI (springdoc 3.2.0) |
| Interface web | HTML, CSS e JavaScript puros, sem build (`src/main/resources/static/index.html`) |
| Build e execução | Maven Wrapper, Docker e Docker Compose |
| Testes | JUnit 6, Mockito, AssertJ, Testcontainers, ArchUnit |

## Como rodar

Pré-requisito: **Docker** com Docker Compose. Não é preciso ter Java nem Maven instalados: o build acontece dentro da imagem.

```bash
cp .env.example .env
docker compose up --build
```

(No `cmd.exe`, use `copy .env.example .env`.) Na primeira vez o build baixa as dependências e leva alguns minutos; a aplicação está pronta quando `docker compose ps` mostra `app` como `healthy`.

| O quê | Onde |
|---|---|
| Interface web | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5433` (banco/usuário `cadastro`) |

Login de desenvolvimento: **`admin` / `admin123`** 

Variáveis do `.env`: `DB_PASSWORD`, `APP_AUTH_USER`, `APP_AUTH_PASSWORD_HASH` (BCrypt, entre aspas simples) e `APP_AUTH_JWT_SECRET` (mínimo de 32 caracteres). Sem elas a aplicação não sobe, de propósito.

## Usando a API

Todas as rotas ficam em `/api/v1` e exigem `Authorization: Bearer <token>`, exceto o login. No Swagger UI, use o botão **Authorize** com o token do login.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/v1/auth/login` | Autentica (`{"usuario","senha"}`) e devolve o token JWT |
| POST | `/api/v1/pessoas` | Cadastra pessoa (`documento` CPF, `nome`, `sobrenome`, `email`) |
| GET | `/api/v1/pessoas?pagina=&tamanho=` | Lista paginada (`pagina` ≥ 1, `tamanho` de 1 a 100, padrão 1 e 20) |
| GET | `/api/v1/pessoas/{documento}` | Busca pelo CPF |
| DELETE | `/api/v1/pessoas/{documento}` | Exclui pelo CPF |
| GET | `/api/v1/pessoas/{documento}/nacionalidade` | Provável nacionalidade do primeiro nome |

O CPF é validado (dígitos verificadores) e aceita máscara (`529.982.247-25`); é guardado só com dígitos e é único.

Cada pessoa tem um `id` (UUID, gerado no cadastro) e um `criadoEm`, a data de criação, devolvido em horário de Brasília (UTC-3, ex.: `2026-10-28T16:02:32.903646-03:00`) e guardado em UTC no banco.

Exemplo:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"usuario":"admin","senha":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s -X POST localhost:8080/api/v1/pessoas -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"documento":"529.982.247-25","nome":"Maria","sobrenome":"Silva","email":"maria@exemplo.com"}'

curl -s localhost:8080/api/v1/pessoas/52998224725/nacionalidade -H "Authorization: Bearer $TOKEN"
```

## Arquitetura

Aplicação em camadas, `controller → service → repository → model`, com as regras de dependência verificadas por ArchUnit. Pacote base: `br.com.testetecnico.cadastro`.

| Pacote | Responsabilidade |
|---|---|
| `controller.v1` | HTTP: leitura da requisição e montagem da resposta |
| `service` | regras de negócio e transações |
| `repository` | acesso ao banco (Spring Data JPA) |
| `model` | `Pessoa` (entidade) e `Documento` (valida o CPF) |
| `client` | integração com a nationalize.io |
| `security` | JWT, limite de tentativas do login |
| `exception` | exceções de negócio e tradução para `problem+json` |

## Testes

```bash
./mvnw verify
```

Exige JDK 21 e Docker em execução (os testes de integração usam Testcontainers com PostgreSQL).
