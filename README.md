# Login Application API

Esta aplicação é uma API de autenticação para gerenciamento de usuários com recuperação de senha via email, utilizando Spring Boot e JWT para autenticação. O objetivo é fornecer um sistema seguro de registro, login, recuperação e redefinição de senha.

## Funcionalidades

- Registro de usuários com validação de email e senha
- Login com autenticação JWT
- Recuperação de senha com envio de link por email
- Redefinição de senha via link com token temporário
- Validação de campos e manipulação de erros

## Tecnologias e Dependências

- **Java 17**
- **Spring Boot 3** (Spring Security, Spring Data JPA, Spring Mail)
- **PostgreSQL** (banco de dados relacional)
- **JWT** para autenticação
- **Mailtrap** (para simulação de envio de emails)
- **BCrypt** para hashing de senhas
- **Maven** para gerenciamento de dependências

## Estrutura do Projeto

- `configs`: Configurações de segurança e criptografia.
- `controllers`: Endpoints da API para autenticação e gerenciamento de usuários.
- `dto`: Data Transfer Objects para validação e transporte de dados entre camadas.
- `entity`: Entidades JPA para o mapeamento do banco de dados.
- `repositories`: Repositórios JPA para operações com o banco de dados.
- `services`: Regras de negócios, gerenciamento de usuários, emails e tokens.
- `utils`: Operações com JWT.

## Pré-requisitos

- **Java 17+**
- **PostgreSQL** (crie um banco de dados para a aplicação e configure as credenciais em `application.properties`)
- **Mailtrap** para captura de emails em ambiente de desenvolvimento (ou outro serviço SMTP)

## Configuração

### Banco de Dados

Configure as credenciais do banco de dados no arquivo `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<NOME_DO_BANCO>
spring.datasource.username=<USUARIO>
spring.datasource.password=<SENHA>
```

### SMTP (Mailtrap)

Utilize suas credenciais do Mailtrap ou outro serviço de SMTP no arquivo `env.properties`:

```properties
spring.mail.username=<USUARIO_MAILTRAP>
spring.mail.password=<SENHA_MAILTRAP>
```

**Obs:** `env.properties` é importado no `application.properties` com a linha `spring.config.import=classpath:env.properties`.

### JWT

As chaves públicas e privadas do JWT são carregadas no `application.properties`. Coloque suas chaves nos arquivos `public.key` e `private.key` na pasta `resources`, e assegure-se de configurar as propriedades:

```properties
jwt.public.key=classpath:public.key
jwt.private.key=classpath:private.key
```

### Configuração do Tempo de Expiração

Defina o tempo de expiração do token JWT e o tempo de expiração do token de redefinição de senha (em segundos):

```properties
jwt.expiration=300
token.expiration.seconds=300
```

## Executando o Projeto

1. Clone o repositório:

   ```bash
   git clone <URL_DO_REPOSITORIO>
   cd login_app
   ```

2. Compile e execute a aplicação com o Maven:

   ```bash
   ./mvnw spring-boot:run
   ```

3. A aplicação estará disponível em `http://localhost:8080`.

## Endpoints da API

### Autenticação

- **POST** `/user/register`: Cria um novo usuário. **Corpo:** `{ "email": "user@example.com", "password": "password" }`
- **POST** `/user/login`: Faz login e retorna um token JWT. **Corpo:** `{ "email": "user@example.com", "password": "password" }`

### Recuperação de Senha

- **POST** `/user/redeem-password`: Envia um email para redefinição de senha. **Corpo:** `{ "email": "user@example.com" }`
- **POST** `/user/reset-password`: Redefine a senha com base em um token de redefinição. **Corpo:** `{ "token": "token_redefinicao", "password": "novaSenha" }`

### Exemplo de Respostas

- **Sucesso**

  ```json
  { "message": "User created successfully" }
  ```

- **Erro**
  ```json
  {
    "errorMessage": "Invalid login credentials",
    "errorStatusCode": "400 BAD_REQUEST"
  }
  ```

## Exceções e Tratamento de Erros

O sistema lida com exceções específicas para validação e autenticação, lançando mensagens amigáveis com o código de status HTTP correspondente.

## Melhorias Futuras

- Implementação de roles diferenciadas e permissões de acesso mais granulares
- Logging detalhado para auditoria e análise de segurança
- Integração com um sistema de gerenciamento de segredos para variáveis sensíveis em produção

## Contribuição

Contribuições são bem-vindas! Siga os passos para criação de pull requests e revisão.
