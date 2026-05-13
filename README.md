# Pokémon Battle Server (Sistema Distribuído)

Simulador de batalhas inspirado no **Pokémon Showdown**, desenvolvido como trabalho prático da disciplina de **Sistemas Distribuídos**.

A aplicação utiliza comunicação via **sockets TCP** em arquitetura cliente-servidor, permitindo que múltiplos clientes se conectem simultaneamente a um servidor central para disputar batalhas pareadas automaticamente por matchmaking.

## Integrantes do Grupo

- Arthur Iwankiu Castro
- Enzo da Silva Passos
- Francklin Roberto dos Santos Junior
- Kayque de Jesus Levy Rodrigues
- Vitor Oliveira Christovam

## Objetivo

Demonstrar na prática conceitos de sistemas distribuídos por meio do desenvolvimento de um servidor de batalhas multiplayer com mecânicas fiéis ao jogo original.

O sistema permite que jogadores:

- Conectem-se a um servidor central via socket TCP
- Entrem em uma fila de matchmaking concorrente
- Sejam pareados automaticamente em batalhas independentes
- Disputem turnos com mecânicas completas de tipo, STAB, velocidade e troca de Pokémon
- Sejam notificados em caso de desconexão do oponente (vitória por W.O.)

## Tecnologias Utilizadas

- **Java puro** (sem frameworks externos)
- **Sockets TCP** (`java.net.Socket` / `ServerSocket`)
- **Threads** (`new Thread(runnable).start()` por cliente)
- **Sincronização** com `synchronized` em métodos críticos de batalha
- **`ConcurrentLinkedQueue`** (`java.util.concurrent`) para a fila de matchmaking thread-safe
- Arquitetura **Cliente-Servidor**

## Estrutura do Projeto

```
src/
├── battle/
│   ├── Battle.java
│   └── TurnManager.java
├── client/
│   └── BatalhaClient.java
├── data/
│   ├── MoveDatabase.java
│   └── PokemonDatabase.java
├── matchmaking/
│   └── Matchmaker.java
├── model/
│   ├── Move.java
│   ├── Pokemon.java
│   └── PokemonSpecies.java
└── server/
    ├── BatalhaServer.java
    └── ClientHandler.java
```

## Funcionamento

1. O servidor (`BatalhaServer.java`) inicia escutando na **porta 5000**.
2. Cada cliente que se conecta recebe um `ClientHandler` próprio, executado em uma **thread separada**, permitindo múltiplas conexões simultâneas.
3. Ao enviar o comando `PLAY`, o jogador é adicionado a uma `ConcurrentLinkedQueue<ClientHandler>`.
4. Quando dois jogadores estão na fila, o servidor os pareia e instancia um objeto `Battle` dedicado para essa partida.
5. Cada batalha é completamente **independente**, com seus próprios times de 3 Pokémon gerados aleatoriamente do banco de dados.
6. Durante a batalha, os jogadores enviam comandos como `MOVE <nome>` ou `SWITCH <nome>`, processados de forma sincronizada pelo servidor.

## Mecânicas de Batalha Implementadas

### Sistema de Turnos Sincronizado
Cada jogador roda em sua própria thread e envia seu movimento via `selectMove()`. O método é `synchronized` e segue o padrão de **barreira dupla**: o turno só é resolvido quando **ambos** os jogadores tiverem escolhido (`moveP1 != null && moveP2 != null`).

### Prioridade por Velocidade
O Pokémon com maior atributo `speed` ataca primeiro. Se o primeiro ataque derrubar o oponente, o segundo movimento não é executado.

### Cálculo de Dano
A fórmula combina:
- Razão **Ataque/Defesa**
- **Poder do movimento**
- **Efetividade de tipo** via `TypeChart.getMultiplier()` (0.0 imune, 0.5 resistência, 1.0 normal, 2.0 super efetivo)
- **STAB** (×1.5 quando o tipo do move coincide com o tipo do Pokémon)
- Variação aleatória de **±15%**

### Troca de Pokémon
A troca consome o turno do jogador. Se o oponente já escolheu um movimento, ele ataca o Pokémon recém-enviado para o campo (`resolveTurnAfterSwitch()`) — comportamento fiel ao jogo original.

### Tratamento de Desconexão
O bloco `finally` em `ClientHandler.run()` garante que `handleDisconnect()` seja sempre chamado, removendo o jogador da fila e notificando o oponente com vitória por **W.O.**

## Estado Atual do Projeto

### ✅ Implementado
- Conexão cliente-servidor via socket TCP
- Sistema de matchmaking concorrente com fila thread-safe
- Múltiplas batalhas simultâneas e independentes
- Sistema de turnos com sincronização via `synchronized` e barreira dupla
- Pokémon reais com stats (Attack, Defense, Speed, HP, Type)
- Moves reais com poder, tipo e efetividade
- Sistema de tipos completo via `TypeChart`
- Cálculo de dano com STAB e variação aleatória
- Sistema de troca de Pokémon
- Tratamento robusto de desconexão (W.O.)

### 🚧 Em Implementação (Mecânicas Finais)
- **Moves de status**: paralisia, sono e queimadura — alteram atributos ou impedem ações por turnos, exigindo expansão de `Pokemon` e da lógica de `resolveTurn()`
- **Sistema de reconexão**: identificação de sessão para reatribuir um cliente ao seu `Battle` em andamento
- **Refinamento e balanceamento** das mecânicas existentes

### 🌐 Plataforma Web (em Finalização)
Migração da comunicação de **sockets TCP puros para WebSockets**, permitindo:
- Conexão direta pelo browser, sem necessidade de rodar `BatalhaClient.java` manualmente
- Interface web com **sprites dos Pokémon**
- **Barras de HP animadas** e feedback visual das batalhas
- Experiência de usuário próxima ao Pokémon Showdown original

## Próximos Passos

- Conclusão da integração WebSocket
- Finalização da interface web com animações
- Implementação completa dos efeitos de status
- Testes finais e ajustes de balanceamento

## Desafios Técnicos Encontrados

- **Sincronização entre threads**: garantir que ambos os jogadores escolham seus movimentos antes da resolução do turno, sem condições de corrida — resolvido com `synchronized` e padrão de barreira dupla.
- **Tratamento de desconexão**: assegurar que o estado da batalha seja limpo e o oponente notificado mesmo em caso de exceção — resolvido com bloco `finally` no `ClientHandler`.

## Conceitos de Sistemas Distribuídos Demonstrados

- Comunicação em rede via sockets TCP
- Concorrência com múltiplas threads
- Sincronização de acesso a recursos compartilhados
- Estruturas de dados thread-safe (`ConcurrentLinkedQueue`)
- Arquitetura cliente-servidor
- Tratamento de falhas de conexão

## Repositório

🔗 [github.com/FrancklinJr/PokemonShowdown](https://github.com/FrancklinJr/PokemonShowdown/tree/main/src)
