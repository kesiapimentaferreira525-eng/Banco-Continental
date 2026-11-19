# Core Bancário — CoreBank API

API REST de operações bancárias desenvolvida com Java 21 e Spring Boot. O projeto permite cadastrar clientes, autenticar por JWT, consultar conta e saldo, realizar depósitos, saques e transferências, além de consultar o extrato paginado.

## Principais funcionalidades

- Cadastro de cliente com validação de CPF, e-mail e senha.
- Criação automática de conta corrente na agência `0001`.
- Login com CPF e senha e geração de token JWT.
- Autenticação stateless por `Authorization: Bearer <token>`.
- Consulta da conta e do saldo do usuário autenticado.
- Depósito, saque e transferência entre contas.
- Extrato paginado com registros de entrada e saída.
- Validações de payload e respostas padronizadas com RFC 7807 (`ProblemDetail`).
- Controle otimista de concorrência nas contas (`@Version`).
- Dados iniciais de demonstração no profile `dev`.
- Documentação interativa com Swagger/OpenAPI.

## Tecnologias

- Java 21
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security
- JWT via `java-jwt` 4.5.0
- Bean Validation / Hibernate Validator
- MySQL 8 / H2 para testes
- Springdoc OpenAPI 3.2.0
- Lombok
- Maven
- Docker Compose

## Arquitetura

```text
src/main/java/corebancario
├── config       # Seed de dados e configuração OpenAPI
├── controller   # Endpoints REST
├── dto          # Requests e responses
├── exception    # Tratamento centralizado de erros
├── model        # Entidades JPA e enums
├── repository   # Repositórios Spring Data JPA
├── security     # JWT, filtro e configuração de segurança
└── service      # Regras de negócio e transações
```

## Modelo de dados

### Customer

Representa o cliente e implementa `UserDetails` do Spring Security.

- `id`: UUID
- `nome`: até 150 caracteres
- `cpf`: 11 dígitos, único
- `email`: único
- `senha`: armazenada com BCrypt
- `contas`: contas associadas

### Account

Representa uma conta bancária.

- `id`: UUID
- `agencia`: padrão `0001`
- `numero`: único, formato gerado `000000-0`
- `tipo`: `CORRENTE`
- `saldo`: `BigDecimal`, precisão 19 e escala 2
- `ativa`: indica se pode receber transferências
- `version`: usada para locking otimista

### Transaction

Registra depósitos, saques e transferências.

- `id`: UUID
- `tipo`: `DEPOSITO`, `SAQUE` ou `TRANSFERENCIA`
- `valor`: `BigDecimal`, precisão 19 e escala 2
- `contaOrigem`: preenchida em saques e transferências
- `contaDestino`: preenchida em depósitos e transferências
- `dataHora`: gerada automaticamente

## Requisitos

- JDK 21
- Maven (ou `./mvnw` / `mvnw.cmd`)
- Docker e Docker Compose, opcional para subir o MySQL
- MySQL 8, caso não use Docker

## Executando o projeto

### 1. Configurar o MySQL com Docker

Na raiz do projeto:

```bash
docker compose up -d
```

O Compose cria o banco `corebank` com usuário `root` e senha padrão `root`, expondo a porta `3306`. A senha pode ser alterada por variável:

```bash
DB_PASSWORD=sua_senha docker compose up -d
```

> O Spring, por padrão, usa o banco `corebancario`, não `corebank`. Ajuste `spring.datasource.url` ou execute o MySQL com a variável correspondente.

### 2. Configurar a aplicação

A configuração principal está em `src/main/resources/application.properties`. Variáveis suportadas:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_USERNAME` | `root` | Usuário do MySQL |
| `DB_PASSWORD` | nenhuma no perfil principal | Senha do MySQL |
| `JWT_SECRET` | chave local de desenvolvimento | Chave HMAC usada para assinar os tokens |
| `JWT_EXPIRATION_HOURS` | `2` | Validade do JWT em horas |
| `PORT` | `8082` | Porta HTTP da API |

Exemplo no PowerShell:

```powershell
$env:DB_PASSWORD="root"
$env:JWT_SECRET="uma-chave-longa-e-segura"
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
export DB_PASSWORD=root
export JWT_SECRET="uma-chave-longa-e-segura"
./mvnw spring-boot:run
```

### 3. Executar em modo de desenvolvimento

O profile `dev` ativa o `DatabaseSeeder` e cria 20 clientes de demonstração.

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

A API inicia em `http://localhost:8082`.

## Usuário de demonstração

Quando o profile `dev` é ativado em um banco vazio:

- Senha de todos os clientes: `123456`
- Primeiro CPF: gerado como `00000000001`
- Conta destino: exibida no log de inicialização

Os 20 clientes recebem saldos iniciais diferentes e são criadas cinco transferências fictícias. O seeder não executa quando já existe pelo menos um cliente.

## Endpoints

Base URL: `http://localhost:8082`

### Autenticação

#### Registrar cliente

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "nome": "Maria Silva",
  "cpf": "12345678909",
  "email": "maria@example.com",
  "senha": "senha123"
}
```

Respostas:

- `201 Created`: cliente e conta criados.
- `400 Bad Request`: nome, CPF, e-mail ou senha inválidos.
- `422 Unprocessable Entity`: CPF ou e-mail já cadastrado.

#### Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "cpf": "00000000001",
  "senha": "123456"
}
```

Resposta `200 OK`:

```json
{
  "token": "eyJ...",
  "tipo": "Bearer",
  "expiraEmSegundos": 7200
}
```

Um CPF enviado com pontuação é normalizado para somente dígitos.

### Conta

#### Consultar minha conta

```http
GET /api/accounts/me
Authorization: Bearer <token>
```

Retorna `id`, agência, número da conta, tipo, saldo e nome do titular.

### Transações

Envie o token em todas as rotas abaixo:

```http
Authorization: Bearer <token>
```

#### Depósito

```http
POST /api/transactions/deposit
Content-Type: application/json
```

```json
{
  "valor": 250.00
}
```

#### Saque

```http
POST /api/transactions/withdraw
Content-Type: application/json
```

```json
{
  "valor": 100.00
}
```

O saque exige saldo suficiente.

#### Transferência

```http
POST /api/transactions/transfer
Content-Type: application/json
```

```json
{
  "contaDestinoNumero": "123456-7",
  "valor": 50.00
}
```

Regras:

- Valor deve ser maior que zero.
- Conta de destino deve existir e estar ativa.
- Não é permitido transferir para a própria conta.
- Saldo da origem deve ser suficiente.
- Origem e destino têm os saldos atualizados na mesma transação.

#### Extrato

```http
GET /api/transactions/statement?page=0&size=10&sort=dataHora,desc
Authorization: Bearer <token>
```

Parâmetros Spring Data:

- `page`: número da página, iniciando em `0`.
- `size`: quantidade por página; padrão `10`.
- `sort`: campo e direção, por exemplo `dataHora,desc`.

O extrato inclui transações em que a conta autenticada aparece como origem ou destino.

### Exemplo de transação

```json
{
  "id": "b31f4d9e-0a4b-4bb8-9e4d-2ab8fa7a8fd3",
  "tipo": "TRANSFERENCIA",
  "valor": 50.00,
  "contaOrigem": "123456-7",
  "contaDestino": "765432-1",
  "dataHora": "2026-11-19T09:33:50"
}
```

Os tipos possíveis são `DEPOSITO`, `SAQUE` e `TRANSFERENCIA`.

## Swagger / OpenAPI

Com a aplicação iniciada:

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Documentação JSON: `http://localhost:8082/v3/api-docs`

No Swagger, use **Authorize** e informe apenas o JWT, sem prefixar novamente com `Bearer`, pois o esquema OpenAPI já utiliza HTTP Bearer.

## Segurança

- Senhas armazenadas com BCrypt.
- Autenticação baseada em token JWT sem estado de sessão.
- CPF é usado como `subject` do token e como identificador do usuário autenticado.
- `POST /api/auth/login` e `POST /api/auth/register` são públicos.
- Swagger também é público.
- Todas as demais rotas exigem autenticação.
- CSRF desabilitado por se tratar de uma API stateless.
- JWT assinado com HMAC256 e emitido pelo issuer `corebank-api`.
- Recomenda-se definir um `JWT_SECRET` forte em ambientes externos.

## Erros e validação

As exceções são tratadas globalmente e retornam `application/problem+json`.

| Situação | HTTP |
|---|---:|
| Payload inválido | `400` |
| Credenciais inválidas | `401` |
| Regra de negócio violada | `422` |
| Conflito por atualização concorrente | `409` |

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Regra de Negócio Violada",
  "status": 422,
  "detail": "Saldo insuficiente para realizar o saque.",
  "timestamp": "2026-11-19T12:33:50Z"
}
```

Erros de validação incluem um campo `erros` com o nome de cada campo e sua mensagem.

## Testes

Os testes têm suporte a perfil H2 em `src/test/resources/application-test.properties`:

- banco em memória `corebanktest`
- H2
- DDL `create-drop`

Executar todos os testes:

```powershell
.\mvnw.cmd test
```

Gerar o JAR:

```powershell
.\mvnw.cmd clean package
```

O artefato será criado em `target/core-bancario-0.0.1-SNAPSHOT.jar`.

> Atualmente, as classes de teste em `corebancario.service` e `corebancario.controller` estão vazias. A estrutura para JUnit/Spring Test já está configurada no projeto e pode ser expandida.

## Banco de dados

A aplicação está configurada para MySQL em `application.properties` e usa `spring.jpa.hibernate.ddl-auto=update`. SQL das operações pode ser exibido porque `spring.jpa.show-sql=true` está habilitado no perfil principal.

Configuração padrão:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/corebancario
spring.datasource.username=root
```

Para H2 em testes:

```properties
spring.datasource.url=jdbc:h2:mem:corebanktest
spring.jpa.hibernate.ddl-auto=create-drop
```

## Estrutura resumida dos diretórios

```text
core-bancario/
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/
│   ├── main/java/corebancario/
│   ├── main/resources/
│   └── test/java/corebancario/
└── target/
```

## Licença

Consulte o arquivo `LICENSE` incluído no repositório.