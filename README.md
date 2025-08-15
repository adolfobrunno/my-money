# My Money

Aplicação Spring Boot para registrar e consultar despesas de forma simples, com integração a WhatsApp (Meta) e extração automática de dados via OpenAI.

## Visão geral

- Recebe mensagens de WhatsApp via Webhook (Meta WhatsApp Cloud API) e persiste cada mensagem recebida em uma fila (coleção Mongo) com status PENDING.
- Um job agendado processa as mensagens PENDING:
  - Identifica o usuário pelo telefone do remetente.
  - Extrai os dados da despesa a partir do texto (OpenAI). Caso a chave do OpenAI seja fake, cai no parser local como fallback para ambiente de desenvolvimento.
  - Cria a Despesa no MongoDB.
  - Envia uma mensagem de confirmação (ou erro) para o usuário no próprio WhatsApp.
- API REST para cadastro e consulta de despesas.
- Documentação OpenAPI/Swagger habilitada.

## Principais componentes

- Webhook WhatsApp: `POST /webhooks/whatsapp` (recebimento) e `GET /webhooks/whatsapp` (verificação hub.challenge do Meta)
- Processador de mensagens: `WhatsAppMessageProcessor` (job a cada 5min)
- Extração por IA: `OpenAIExpenseExtractor` (usa JSON Schema e retorna objeto Despesa)
- Persistência: Spring Data MongoDB
- Segurança: endpoints públicos mínimos para Webhook; demais rotas autenticadas

## Tecnologias

- Java 21
- Spring Boot 3.5
- MongoDB
- Gradle
- Docker Compose (MongoDB para dev)

## Pré‑requisitos

- Java 21
- Docker e Docker Compose (opcional, para subir o Mongo)
- MongoDB disponível (local ou em container)

## Configuração

As configurações padrão estão em `src/main/resources/application.yml` e podem ser sobrescritas via variáveis de ambiente.

WhatsApp (Meta):
- META_WHATSAPP_VERIFY_TOKEN (default: fAkE_vErIfY_tOkEn)
- META_WHATSAPP_ACCESS_TOKEN (default: FAKE_ACCESS_TOKEN)
- META_WHATSAPP_PHONE_NUMBER_ID (default: 000000000000000)
- META_WHATSAPP_APP_SECRET (default: FAKE_APP_SECRET)

OpenAI:
- OPENAI_API_KEY (default: FAKE_OPENAI_API_KEY)
- OPENAI_BASE_URL (default: https://api.openai.com/v1)
- OPENAI_MODEL (default: gpt-4o-mini)

Observações:
- Com `OPENAI_API_KEY` fake, o sistema não chama a API externa; usa um parser local para continuar funcionando em desenvolvimento.
- Para que o processamento de mensagens funcione, o número de telefone do usuário cadastrado deve coincidir com o `from` das mensagens recebidas (apenas dígitos, ex.: 5511999999999).

## Como executar

1) Subir o MongoDB com Docker (opcional):

- Arquivo: `compose.yaml` já inclui um serviço `mongodb`.
- Com Docker Desktop em execução, rode na raiz do projeto:
  - `docker compose up -d`

2) Executar a aplicação localmente com Gradle:

- Windows (PowerShell):
  - `./gradlew.bat bootRun`
- Linux/macOS:
  - `./gradlew bootRun`

A aplicação iniciará em `http://localhost:8080`.

3) Ver a documentação OpenAPI/Swagger:
- `http://localhost:8080/swagger-ui/index.html`

## Webhook do WhatsApp

- Recebimento: `POST /webhooks/whatsapp` — o sistema salva a mensagem e retorna 202.
- Verificação: `GET /webhooks/whatsapp` — responde ao `hub.challenge` usando o `META_WHATSAPP_VERIFY_TOKEN` configurado.

Formato de mensagens aceitas (exemplos):
- "Despesa: Almoço; Valor: 35,90; Pagamento: PIX"
- "Mercado | 120.50 | CARTAO CREDITO"

Formas de pagamento aceitas: `DINHEIRO`, `PIX`, `CARTAO_CREDITO`, `CARTAO_DEBITO`.

## Endpoints úteis

- POST `/api/auth/register` — cadastro de usuário
- POST `/api/auth/login` — login
- GET `/api/despesas` — lista despesas do usuário autenticado
- POST `/api/despesas` — cria despesa

(Algumas páginas estáticas de exemplo estão em `src/main/resources/static`.)

## Executar testes

- `./gradlew.bat test` (Windows) ou `./gradlew test` (Linux/macOS)

## Dicas e troubleshooting

- Se o job não estiver processando mensagens: verifique se há usuários com `telefone` correspondente ao número `from` do WhatsApp (somente dígitos) e se o Mongo está acessível.
- Se a integração com OpenAI falhar e você estiver com `OPENAI_API_KEY` válida: confira `OPENAI_BASE_URL` e `OPENAI_MODEL`. Logs conterão mensagens de erro do `OpenAIExpenseExtractor`.
- Para ambientes sem Internet ou sem credenciais, mantenha a chave fake e use os formatos de mensagem de exemplo para testar o parser local.

## Licença

Este projeto é de uso interno/experimental. Ajuste conforme suas necessidades.
