# Segurança

Este documento descreve as decisões de segurança adotadas no projeto, as
ameaças que cada uma mitiga e como configurar o ambiente corretamente.

## Configuração obrigatória antes de rodar

Crie um arquivo `.env` na raiz do projeto (copie de `.env.example`) e gere
segredos reais:

```bash
# Gere uma SECRET_KEY do Django
python -c "import secrets; print(secrets.token_urlsafe(50))"

# Gere um token de autenticacao Django <-> Java
openssl rand -hex 32
```

Exporte as variáveis antes de subir os processos:

```bash
export DJANGO_SECRET_KEY="..."
export BATALHA_AUTH_TOKEN="..."
export DJANGO_DEBUG=1               # 0 em producao
```

Ou use uma ferramenta como `python-decouple`, `direnv` ou o próprio
`source .env` em desenvolvimento.

## Camadas de defesa implementadas

### 1. Servidor Java isolado em `127.0.0.1`

O `BatalhaServer` faz bind apenas no loopback. Nenhuma conexão TCP vinda da
rede externa consegue alcançar a porta 5000 diretamente — o único caminho
para entrar em uma batalha é passar pelo Django.

**Mitiga:** acesso direto ao servidor Java sem autenticação, conexão TCP
arbitrária da internet, varredura de porta a partir de máquinas externas.

### 2. Token compartilhado Django ↔ Java

A primeira mensagem que o Django envia ao abrir o socket TCP é
`AUTH <token>`. O Java rejeita qualquer outra coisa antes da autenticação,
encerrando a conexão sem revelar o motivo.

A comparação do token usa tempo constante (`constantTimeEquals`) para evitar
**timing attacks**.

**Mitiga:** mesmo que alguém consiga acesso ao loopback (ex: outro processo
na mesma máquina, container vizinho), não consegue falar com o servidor Java
sem o token.

### 3. Validação de comandos no WebSocket

O `BatalhaConsumer` só aceita os verbos `PLAY`, `MOVE` e `SWITCH`. Qualquer
tentativa de injetar `AUTH`, `TEAM`, ou comandos com `\n` é descartada.
Argumentos passam por uma regex restrita (`[A-Za-z0-9 \-]+`) e por limite de
tamanho.

**Mitiga:** um cliente malicioso que tente injetar comandos extras na mesma
mensagem (CRLF injection) ou enviar um comando privilegiado direto.

### 4. Validação de stats no `PokemonFactory`

Todos os campos vindos do payload `TEAM` passam por:

- `clamp` em limites razoáveis (HP ≤ 300, atk/def/spd ≤ 200, power ≤ 200)
- `parseTypeSafe` que cai em `NORMAL` se o tipo não existir no enum
- `sanitize` que remove caracteres de controle e separadores do protocolo
- `sanitizeSprite` que só aceita URLs `http(s)://` com tamanho ≤ 512

**Mitiga:** cheating (Pokémon com atk = 9999), injeção de quebra de linha
nos nomes (que poderia bagunçar o protocolo), e XSS via `javascript:` ou
`data:` na URL do sprite ao renderizar no front.

### 5. Rate limiting

- **WebSocket:** 5 comandos por segundo por conexão.
- **Servidor Java:** 10 comandos por segundo por conexão TCP.
- **Tamanho máximo de mensagem:** 2 KB no WebSocket, 4 KB no Java.

**Mitiga:** flood de comandos, DoS por consumo de CPU, abuso da PokéAPI ao
mandar `PLAY` em loop.

### 6. Segredos fora do Git

- `SECRET_KEY` do Django agora vem de `DJANGO_SECRET_KEY`.
- Token de autenticação vem de `BATALHA_AUTH_TOKEN`.
- `.gitignore` reforçado para `.env`, `*.secret`, `*.key`, `db.sqlite3`.
- Template público em `.env.example`.

**Mitiga:** vazamento de credenciais via repositório público — quem clonar
o projeto não recebe a chave de produção junto.

> **Importante:** a `SECRET_KEY` que estava commitada antes deve ser
> considerada **comprometida**. Gere uma nova e nunca reaproveite a antiga.

### 7. Cabeçalhos de segurança em produção

Quando `DJANGO_DEBUG=0`, o `settings.py` ativa automaticamente:

- `SECURE_HSTS_SECONDS = 31536000` (HSTS de 1 ano)
- `SECURE_SSL_REDIRECT = True`
- `SESSION_COOKIE_SECURE` e `CSRF_COOKIE_SECURE`
- `X_FRAME_OPTIONS = DENY` (anti-clickjacking)
- `SECURE_CONTENT_TYPE_NOSNIFF`

**Mitiga:** downgrade para HTTP, sequestro de sessão em rede insegura,
clickjacking, MIME sniffing.

## Pontos de atenção que ficaram fora deste lote

Para um próximo ciclo, considere:

- **Autenticação real de usuários** (login/cadastro) em vez de sessão
  anônima — útil se quiserem ladder/ranking.
- **WSS (WebSocket sobre TLS)** em produção, com Nginx + Let's Encrypt na
  frente do Daphne.
- **Limite de conexões por IP** no Java (hoje só limita por conexão).
- **Logs de auditoria** persistidos em arquivo separado.
- **Reconexão por sessão** (já mapeada como mecânica futura no README).

## Resposta a incidentes

Se suspeitar que o token vazou:

1. Pare o servidor Java e o Django.
2. Gere um novo `BATALHA_AUTH_TOKEN` (`openssl rand -hex 32`).
3. Atualize a variável de ambiente nos dois processos.
4. Suba os dois novamente — qualquer conexão antiga será recusada.

Se a `SECRET_KEY` do Django vazar:

1. Gere uma nova com `secrets.token_urlsafe(50)`.
2. Atualize `DJANGO_SECRET_KEY`.
3. Force logout de todos os usuários (limpe a tabela `django_session`).
