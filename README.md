# Core Bancário — CoreBank API

API REST de operações bancárias desenvolvida com Java 21 e Spring Boot. Permite cadastrar clientes, autenticar por JWT, consultar conta e saldo, realizar depósitos, saques e transferências e consultar o extrato paginado.

## Funcionalidades

- Cadastro de cliente com validação de CPF, e-mail e senha.
- Criação automática de conta corrente na agência `0001`.
- Login com CPF e senha e geração de token JWT.
- Autenticação stateless com `Authorization: Bearer <token>`.
- Consulta da conta e do saldo do usuário autenticado.
- Depósito, saque e transferência entre contas.
- Extrato paginado com registros de entrada e saída.
- Validação de payload e respostas RFC 7807 (`ProblemDetail`).
- Controle otimista de concorrência com `@Version`.
- Dados iniciais de demonstração no profile `dev`.
- Documentação interativa Swagger/OpenAPI.

## Tecnologias

- Java 21
- Spring Boot 4.1.2
- Spring Web MVC e Spring Data JPA
- Spring Security
- JWT via `java-jwt` 4.5.0
- Bean Validation / Hibernate Validator
- MySQL 8 e H2 para testes
- Springdoc OpenAPI 3.2.0
- Lombok e Maven
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

- `id`: UUID
- `nome`: até 150 caracteres
- `cpf`: 11 dígitos, único
- `email`: único
- `senha`: armazenada com BCrypt
- Implementa `UserDetails` do Spring Security

### Account

- `id`: UUID
- `agencia`: padrão `0001`
- `numero`: único, formato `000000-0`
- `tipo`: `CORRENTE`
- `saldo`: `BigDecimal`, precisão 19 e escala 2
- `ativa`: indica se pode receber transferências
- `version`: locking otimista

### Transaction

- `id`: UUID
- `tipo`: `DEPOSITO`, `SAQUE` ou `TRANSFERENCIA`
- `valor`: `BigDecimal`, precisão 19 e escala 2
- `contaOrigem`: saques e transferências
- `contaDestino`: depósitos e transferências
- `dataHora`: gerada automaticamente

## Requisitos

- JDK 21
- Maven ou os wrappers `mvnw` / `mvnw.cmd`
- Docker e Docker Compose, opcional
- MySQL 8, caso não utilize Docker

## Executando

### 1. Subir o MySQL

```powershell
docker compose up -d
```

O Compose cria o banco `corebank`, usuário `root` e senha padrão `root`, na porta `3306`. Para alterar a senha:

```powershell
$env:DB_PASSWORD="sua_senha"
docker compose up -d
```

### 2. Configurar variáveis

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_USERNAME` | `root` | Usuário do MySQL |
| `DB_PASSWORD` | nenhuma no perfil principal | Senha do MySQL |
| `JWT_SECRET` | chave local de desenvolvimento | Chave HMAC dos tokens |
| `JWT_EXPIRATION_HOURS` | `2` | Validade do JWT |
| `PORT` | `8082` | Porta HTTP |

Exemplo no Windows:

```powershell
$env:DB_PASSWORD="root"
$env:JWT_SECRET="uma-chave-longa-e-segura"
.\mvnw.cmd spring-boot:run
```

Exemplo no Linux/macOS:

```bash
export DB_PASSWORD=root
export JWT_SECRET="uma-chave-longa-e-segura"
./mvnw spring-boot:run
```

A API ficará disponível em `http://localhost:8082`.

### 3. Executar com dados de demonstração

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

O seeder cria 20 clientes somente se o banco estiver vazio. A senha é `123456`; o primeiro CPF é gerado como `00000000001`. Os dados de acesso são exibidos no log de inicialização.

## Endpoints

Base URL: `http://localhost:8082`

### Registrar cliente

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

Respostas: `201 Created`, `400 Bad Request` ou `422 Unprocessable Entity`.

### Login

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

Resposta:

```json
{
  "token": "eyJ...",
  "tipo": "Bearer",
  "expiraEmSegundos": 7200
}
```

### Consultar conta

```http
GET /api/accounts/me
Authorization: Bearer <token>
```

Retorna dados da conta e saldo do usuário autenticado.

### Depósito

```http
POST /api/transactions/deposit
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "valor": 250.00
}
```

### Saque

```http
POST /api/transactions/withdraw
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "valor": 100.00
}
```

Exige saldo suficiente.

### Transferência

```http
POST /api/transactions/transfer
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "contaDestinoNumero": "123456-7",
  "valor": 50.00
}
```

Regras:

- Valor maior que zero.
- Conta destino existente e ativa.
- Não é permitido transferir para a própria conta.
- Saldo da origem deve ser suficiente.
- Origem e destino são atualizados na mesma transação.

### Extrato

```http
GET /api/transactions/statement?page=0&size=10&sort=dataHora,desc
Authorization: Bearer <token>
```

- `page`: página iniciando em `0`.
- `size`: itens por página; padrão `10`.
- `sort`: campo e direção, por exemplo `dataHora,desc`.

## Swagger/OpenAPI

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Documentação JSON: `http://localhost:8082/v3/api-docs`

Use **Authorize** e informe apenas o JWT. O esquema Swagger já adiciona `Bearer`.

## Segurança

- Senhas armazenadas com BCrypt.
- API stateless com JWT.
- CPF é o subject do token e o identificador do usuário autenticado.
- Login e registro são públicos.
- Swagger é público.
- Demais endpoints exigem autenticação.
- CSRF desabilitado por se tratar de API REST stateless.
- JWT assinado com HMAC256 e issuer `corebank-api`.
- Use um `JWT_SECRET` forte fora do ambiente local.

## Erros

As exceções são tratadas globalmente com `application/problem+json`.

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

## Testes

O perfil de teste utiliza H2 em memória:

```properties
spring.datasource.url=jdbc:h2:mem:corebanktest
spring.jpa.hibernate.ddl-auto=create-drop
```

Executar:

```powershell
.\mvnw.cmd test
```

Gerar o JAR:

```powershell
.\mvnw.cmd clean package
```

> Atualmente, as classes de teste em `corebancario.service` e `corebancario.controller` estão vazias. O projeto já possui as dependências necessárias para ampliá-las.

## Banco de dados

Perfil principal configurado para MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/corebancario
spring.datasource.username=root
spring.jpa.hibernate.ddl-auto=update
```

Atenção: o `docker-compose.yml` cria o banco `corebank`, enquanto `application.properties` aponta para `corebancario`. Ajuste uma das configurações antes de iniciar.

## Licença

Consulte o arquivo `LICENSE` incluído no repositório.
