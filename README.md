# Pokémon Battle Server (Sistema Distribuído)

Simulador de batalhas inspirado no **Pokémon Showdown**, desenvolvido como trabalho prático da disciplina de **Sistemas Distribuídos**.

O sistema combina um **servidor de batalha em Java** (responsável pela lógica de jogo via sockets TCP) com uma **camada web em Django + Channels** que expõe uma interface no browser através de WebSockets e integra-se à **PokéAPI** para gerar times dinamicamente.

## Integrantes do Grupo

- Arthur Iwankiu Castro
- Enzo da Silva Passos
- Francklin Roberto dos Santos Junior
- Kayque de Jesus Levy Rodrigues
- Vitor Oliveira Christovam

## Arquitetura

O projeto adota uma **arquitetura distribuída em três camadas**:

```
┌─────────────┐    WebSocket    ┌───────────────────┐    TCP Socket    ┌────────────────┐
│   Browser   │ ◄────────────► │ Django + Channels │ ◄──────────────► │ Servidor Java  │
│ (HTML/JS)   │     JSON       │   (Python ASGI)   │  TEAM/MOVE/etc.  │   (porta 5000) │
└─────────────┘                └───────────────────┘                  └────────────────┘
                                        │
                                        ▼
                                  ┌──────────┐
                                  │ PokéAPI  │
                                  └──────────┘
```

- **Servidor Java**: núcleo do jogo. Gerencia conexões TCP, matchmaking, batalhas, turnos e regras.
- **Camada Django/Channels**: ponte entre o browser e o servidor Java. Cada cliente WebSocket abre uma conexão TCP dedicada ao servidor Java.
- **PokéAPI**: fornece dados reais (stats, sprites, moves) para os Pokémon dos times.

## Estrutura do Projeto

```
PokemonShowdown/
├── server/                          # Servidor de batalha Java
│   └── src/
│       ├── server/
│       │   ├── BatalhaServer.java   # ServerSocket porta 5000
│       │   ├── ClientHandler.java   # Thread por cliente
│       │   └── JsonMessage.java     # Builder de mensagens JSON
│       ├── battle/
│       │   ├── Battle.java          # Lógica completa da batalha
│       │   └── TurnManager.java
│       ├── matchmaking/
│       │   └── Matchmaker.java
│       ├── model/
│       │   ├── Pokemon.java
│       │   ├── PokemonSpecies.java
│       │   ├── Move.java
│       │   ├── Type.java            # NORMAL, FIRE, WATER, GRASS, ELECTRIC, PSYCHIC, GHOST
│       │   └── TypeChart.java       # Multiplicadores de efetividade
│       ├── data/
│       │   ├── PokemonDatabase.java # Pool local (Pikachu, Charizard, etc.)
│       │   ├── PokemonFactory.java  # Constrói Pokémon a partir de string TEAM
│       │   └── MoveDatabase.java
│       └── client/
│           └── BatalhaClient.java   # Cliente Java legado (terminal)
│
└── web/                             # Plataforma web Django
    ├── manage.py
    ├── pokemon_web/
    │   ├── settings.py              # Django 6.0 + channels
    │   ├── asgi.py                  # ProtocolTypeRouter HTTP/WebSocket
    │   └── urls.py
    └── batalha/
        ├── consumers.py             # BatalhaConsumer (WebSocket ↔ TCP Java)
        ├── routing.py               # ws/batalha/
        ├── pokeapi.py               # Integração com PokéAPI
        ├── views.py
        └── templates/batalha/
            └── index.html           # Front-end com sprites e barras de HP
```

## Tecnologias Utilizadas

**Servidor de Batalha**
- Java puro (sem frameworks externos)
- `java.net.Socket` / `ServerSocket` (TCP)
- Threads (`new Thread(runnable).start()` por cliente)
- `synchronized` para sincronização de turnos
- `ConcurrentLinkedQueue` para fila de matchmaking thread-safe

**Plataforma Web**
- Django 6.0
- Django Channels (suporte a WebSocket via ASGI)
- `requests` (consumo da PokéAPI)
- HTML, CSS e JavaScript puro no front-end

**Dados Externos**
- [PokéAPI](https://pokeapi.co) — stats, sprites e moves dinâmicos dos 151 Pokémon da primeira geração

## Protocolo de Comunicação

### Browser → Django (WebSocket, JSON)
```json
{ "comando": "PLAY" }
{ "comando": "MOVE Thunderbolt" }
{ "comando": "SWITCH Charizard" }
```

### Django → Servidor Java (TCP, texto)
```
TEAM Pikachu,35,55,40,90,ELECTRIC,<url_sprite>,Thunderbolt:90:ELECTRIC|...;Charizard,...
MOVE Thunderbolt
SWITCH Charizard
```

### Servidor Java → Browser (JSON via Django)
O `JsonMessage.java` produz quatro tipos de mensagem:

| Tipo          | Payload                                                              |
| ------------- | -------------------------------------------------------------------- |
| `log`         | `{ "tipo": "log", "mensagem": "..." }`                               |
| `aguardando`  | `{ "tipo": "aguardando" }`                                           |
| `estado`      | Snapshot completo: HP, stats, tipo, sprite, moves, time, ativo, etc. |
| `fim`         | `{ "tipo": "fim", "resultado": "vitoria" \| "derrota" \| "wo" }`     |

## Funcionamento

1. O servidor Java (`BatalhaServer.java`) inicia escutando na **porta 5000**.
2. O servidor Django sobe (ASGI/Daphne ou `runserver`) e expõe `ws/batalha/` para conexões WebSocket.
3. O usuário abre o navegador e conecta-se via WebSocket ao `BatalhaConsumer`.
4. Ao enviar `PLAY`, o consumer:
   - Chama `buscar_time_aleatorio()` na PokéAPI para montar um time de 3 Pokémon
   - Coloca o jogador na fila local do Django (`fila` + `asyncio.Lock`)
   - Quando há dois jogadores, abre **uma conexão TCP por jogador** com o servidor Java
   - Envia o comando `TEAM <dados>` para cada conexão
5. O servidor Java pareia os dois `ClientHandler` em uma instância de `Battle` independente.
6. Durante a partida, os comandos `MOVE` e `SWITCH` viajam Browser → Django → Java; as respostas JSON voltam pelo caminho inverso.

## Mecânicas de Batalha Implementadas

### Sistema de Turnos Sincronizado
O método `selectMove()` é `synchronized` e usa o padrão de **barreira dupla**: o turno só é resolvido quando ambos os jogadores tiverem escolhido (`moveP1 != null && moveP2 != null`). Enquanto isso, quem já escolheu recebe `JsonMessage.aguardando()`.

### Prioridade por Velocidade
Em `resolveTurn()`, quem tem maior `speed` ataca primeiro. Se o primeiro ataque derrubar o oponente (`isFainted()`), o segundo movimento não é executado.

### Cálculo de Dano
```java
base = (attack / defense) * power * 0.1 * effectiveness * stab
damage = base * (0.85 a 1.0)
```
Combinando:
- Razão **Ataque/Defesa**
- **Poder do movimento**
- **Efetividade de tipo** via `TypeChart.getMultiplier()` (0.0 imune, 0.5 resistência, 1.0 normal, 2.0 super efetivo)
- **STAB** ×1.5 quando o tipo do move coincide com o tipo do atacante
- Variação aleatória de **±15%**

Tipos atualmente suportados: `NORMAL`, `FIRE`, `WATER`, `GRASS`, `ELECTRIC`, `PSYCHIC`, `GHOST`.

### Troca de Pokémon
A troca consome o turno do jogador. Se o oponente já escolheu um movimento, ele ataca o Pokémon recém-enviado para o campo (`resolveTurnAfterSwitch()`). O sistema usa o valor sentinela `moveP = -1` para sinalizar "este jogador trocou em vez de atacar".

### Tratamento de Desconexão
O bloco `finally` em `ClientHandler.run()` garante que `handleDisconnect()` seja sempre chamado, removendo o jogador da fila do Java e notificando o oponente com `JsonMessage.fim("wo")`.

## Como Executar

### 1. Servidor Java
```bash
cd server/src
javac server/BatalhaServer.java server/ClientHandler.java server/JsonMessage.java \
      battle/*.java model/*.java data/*.java matchmaking/*.java
java server.BatalhaServer
```
Saída esperada: `Servidor iniciado na porta 5000`

### 2. Servidor Web (em outro terminal)
```bash
cd web
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install django channels requests
python manage.py runserver
```
Acesse `http://localhost:8000` no navegador para entrar na fila.

> ⚠️ Para suporte completo a WebSocket em produção, é recomendado rodar com **Daphne** (`daphne pokemon_web.asgi:application`) em vez de `runserver`.

## Estado Atual do Projeto

### ✅ Implementado
- Servidor de batalha Java com matchmaking, threads e sincronização
- Múltiplas batalhas simultâneas e independentes
- Sistema de turnos com `synchronized` + barreira dupla
- Cálculo de dano com efetividade de tipo, STAB e variação aleatória
- Sistema completo de troca de Pokémon (incluindo casos com oponente já decidido)
- Tratamento robusto de desconexão (vitória por W.O.)
- Protocolo JSON estruturado (`log`, `estado`, `aguardando`, `fim`)
- Camada Django + Channels com WebSocket
- Integração com **PokéAPI** (times reais com sprites, stats e moves dinâmicos)
- Front-end web com sprites dos Pokémon e barras de HP animadas
- 7 tipos de Pokémon e tabela de efetividade

### 🚧 Em Implementação (Mecânicas Finais)
- **Moves de status**: paralisia, sono e queimadura — alterar atributos / impedir ações por turnos
- **Sistema de reconexão por sessão**: reatribuir um cliente ao seu `Battle` em andamento
- **Refinamento e balanceamento** das mecânicas existentes

### 🌐 Plataforma Web (em Finalização)
- Polimento visual da interface
- Animações de ataque
- Mensagens de log estilizadas
- Indicadores de turno e estado mais claros

## Desafios Técnicos Encontrados

- **Sincronização entre threads**: garantir que ambos os jogadores escolham antes da resolução do turno — resolvido com `synchronized` + barreira dupla.
- **Tratamento de desconexão**: assegurar que o estado da batalha seja limpo e o oponente notificado mesmo em caso de exceção — resolvido com bloco `finally` no `ClientHandler`.
- **Ponte entre paradigmas assíncrono e bloqueante**: o `BatalhaConsumer` (asyncio) precisa ler de um socket TCP bloqueante. Resolvido com uma **thread listener** dedicada que usa `asyncio.run_coroutine_threadsafe` para enviar mensagens de volta ao WebSocket.
- **Tradução de dados da PokéAPI para o protocolo interno**: muitos moves da API não têm `power` (status moves) — filtrados com fallback para `Tackle`, `Quick Attack`, etc., garantindo sempre 4 moves por Pokémon.

## Conceitos de Sistemas Distribuídos Demonstrados

- Comunicação em rede via sockets TCP e WebSocket
- Concorrência com múltiplas threads (Java) e corrotinas (Python)
- Sincronização de acesso a recursos compartilhados
- Estruturas de dados thread-safe (`ConcurrentLinkedQueue`, `asyncio.Lock`)
- Arquitetura cliente-servidor em múltiplas camadas
- Integração com API externa (PokéAPI)
- Tratamento de falhas de conexão

## Repositório

🔗 [github.com/FrancklinJr/PokemonShowdown](https://github.com/FrancklinJr/PokemonShowdown)
