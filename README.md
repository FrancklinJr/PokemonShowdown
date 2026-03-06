# Pokémon Battle Server (Sistema Distribuído)

Este projeto é um simulador de batalhas inspirado no Pokémon Showdown, desenvolvido como trabalho prático da disciplina de Sistemas Distribuídos.

A proposta é criar uma aplicação distribuída utilizando comunicação via sockets, onde múltiplos clientes podem se conectar a um servidor central para disputar batalhas.

## Objetivo

Demonstrar conceitos de sistemas distribuídos através do desenvolvimento de um servidor de batalhas multiplayer.

O sistema permitirá que jogadores:

- Conectem-se a um servidor
- Entrem em uma fila de matchmaking
- Iniciem batalhas contra outros jogadores
- Troquem comandos de batalha através do servidor

## Tecnologias utilizadas

- Java
- Sockets (TCP)
- Arquitetura Cliente-Servidor

## Estrutura atual do projeto

client/
- BatalhaClient.java

server/
- BattleServer.java
- ClientHandler.java

## Funcionamento atual

1. O servidor inicia e fica aguardando conexões.
2. Um cliente se conecta ao servidor.
3. O jogador envia o comando `PLAY`.
4. O servidor coloca o jogador em uma fila de matchmaking.
5. Quando dois jogadores entram na fila, o servidor inicia uma batalha.
6. Durante a batalha os jogadores enviam comandos como `MOVE`.

## Estado atual do projeto

Implementado:

- Conexão cliente-servidor via socket
- Sistema de matchmaking
- Comunicação entre jogadores através do servidor
- Sistema simples de batalha com turnos
- Ataques básicos e sistema de HP

## Próximos passos

- Implementar Pokémon reais
- Adicionar moves reais
- Criar sistema de stats (Attack, Defense, Speed, etc.)
- Implementar múltiplas batalhas simultâneas
- Melhorar o protocolo de comunicação entre cliente e servidor

## Objetivo final

Criar um simulador de batalhas Pokémon simplificado que demonstre na prática conceitos de:

- Sistemas distribuídos
- Comunicação em rede
- Concorrência
- Arquitetura cliente-servidor
