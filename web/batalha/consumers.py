import json
import socket
import asyncio
import threading
from channels.generic.websocket import AsyncWebsocketConsumer
from batalha.pokeapi import buscar_time_aleatorio

JAVA_HOST = 'localhost'
JAVA_PORT = 5000

fila = []
fila_lock = asyncio.Lock()

class BatalhaConsumer(AsyncWebsocketConsumer):

    async def connect(self):
        await self.accept()
        self.running = True
        self.loop = asyncio.get_running_loop()
        self.java_socket = None
        print("Jogador conectado.")

    async def disconnect(self, code):
        self.running = False
        if self.java_socket:
            try:
                self.java_socket.close()
            except:
                pass
        print("Jogador desconectado.")

    async def receive(self, text_data):
        data = json.loads(text_data)
        comando = data.get('comando', '')
        print(f"Comando: {comando}")

        if comando == 'PLAY':
            await self.entrar_na_fila()
        else:
            if self.java_socket:
                self.java_socket.sendall((comando + '\n').encode('utf-8'))

    async def entrar_na_fila(self):
        await self.send(text_data=json.dumps(
            {'tipo': 'log', 'mensagem': 'Buscando Pokémon na PokéAPI...'}
        ))

        team = await asyncio.get_event_loop().run_in_executor(
            None, buscar_time_aleatorio
        )
        self.team = team

        await self.send(text_data=json.dumps(
            {'tipo': 'log', 'mensagem': 'Time montado! Procurando oponente...'}
        ))

        async with fila_lock:
            if fila:
                opponent = fila.pop(0)

                await self.send(text_data=json.dumps(
                    {'tipo': 'log', 'mensagem': 'Partida encontrada!'}
                ))
                await opponent['ws'].send(text_data=json.dumps(
                    {'tipo': 'log', 'mensagem': 'Partida encontrada!'}
                ))

                await self.conectar_java()
                await opponent['ws'].conectar_java()

                team1_str = montar_team_str(self.team)
                team2_str = montar_team_str(opponent['ws'].team)

                self.java_socket.sendall(f'TEAM {team1_str}\n'.encode())
                opponent['ws'].java_socket.sendall(f'TEAM {team2_str}\n'.encode())

            else:
                fila.append({'ws': self})
                await self.send(text_data=json.dumps(
                    {'tipo': 'log', 'mensagem': 'Aguardando oponente...'}
                ))

    async def conectar_java(self):
        self.java_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.java_socket.connect((JAVA_HOST, JAVA_PORT))
        self.java_file = self.java_socket.makefile('r', encoding='utf-8')

        self.listener = threading.Thread(target=self._listen_java, daemon=True)
        self.listener.start()

    def _listen_java(self):
        try:
            for linha in self.java_file:
                linha = linha.strip()
                if not linha or not self.running:
                    continue
                print(f"Java → browser: {linha}")
                future = asyncio.run_coroutine_threadsafe(
                    self.send(text_data=linha),
                    self.loop
                )
                future.result(timeout=5)
        except Exception as e:
            print(f"Listener encerrado: {e}")


def montar_team_str(team):
    pokemons = []
    for p in team:
        moves_str = '|'.join(
            f"{m['nome']}:{m['poder']}:{m['tipo']}" for m in p['moves']
        )
        sprite = p.get('sprite') or ''
        pokemons.append(
            f"{p['nome']},{p['hp']},{p['atk']},{p['def']},{p['spd']},{p['tipo']},{sprite},{moves_str}"
        )
    return ';'.join(pokemons)