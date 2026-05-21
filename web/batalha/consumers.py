import json
import os
import re
import socket
import asyncio
import threading
import time
from channels.generic.websocket import AsyncWebsocketConsumer
from batalha.pokeapi import buscar_time_aleatorio

JAVA_HOST = os.environ.get('JAVA_HOST', '127.0.0.1')
JAVA_PORT = int(os.environ.get('JAVA_PORT', '5000'))

BATALHA_AUTH_TOKEN = os.environ.get('BATALHA_AUTH_TOKEN')

COMANDOS_PERMITIDOS = {'PLAY', 'MOVE', 'SWITCH'}

MAX_COMANDOS_POR_SEGUNDO = 5

MAX_MSG_BYTES = 2048

MAX_ARG_LEN = 64

fila = []
fila_lock = asyncio.Lock()


def comando_sanitizado(raw_comando: str):
    if not isinstance(raw_comando, str):
        return None
    raw_comando = raw_comando.strip()
    if not raw_comando or len(raw_comando) > MAX_MSG_BYTES:
        return None

    if '\n' in raw_comando or '\r' in raw_comando:
        return None

    partes = raw_comando.split(' ', 1)
    verbo = partes[0].upper()
    if verbo not in COMANDOS_PERMITIDOS:
        return None

    if verbo == 'PLAY':
        if len(partes) > 1 and partes[1].strip():
            return None
        return ('PLAY', '')

    if len(partes) < 2:
        return None

    arg = partes[1].strip()
    if not arg or len(arg) > MAX_ARG_LEN:
        return None
    if not re.fullmatch(r'[A-Za-z0-9 \-]+', arg):
        return None

    return (verbo, arg)


class BatalhaConsumer(AsyncWebsocketConsumer):

    async def connect(self):
        if not BATALHA_AUTH_TOKEN:
            await self.close(code=4500)
            return
        await self.accept()
        self.running = True
        self.loop = asyncio.get_running_loop()
        self.java_socket = None
        self.in_queue = False
        self._janela_inicio = 0.0
        self._comandos_na_janela = 0
        print("Jogador conectado.")

    async def disconnect(self, code):
        self.running = False
        if self.java_socket:
            try:
                self.java_socket.close()
            except Exception:
                pass
        async with fila_lock:
            global fila
            fila = [item for item in fila if item['ws'] is not self]
        print("Jogador desconectado.")

    def _rate_limit_ok(self) -> bool:
        agora = time.monotonic()
        if agora - self._janela_inicio >= 1.0:
            self._janela_inicio = agora
            self._comandos_na_janela = 1
            return True
        self._comandos_na_janela += 1
        return self._comandos_na_janela <= MAX_COMANDOS_POR_SEGUNDO

    async def receive(self, text_data=None, bytes_data=None):
        if bytes_data is not None:
            return
        if text_data is None or len(text_data) > MAX_MSG_BYTES:
            return

        if not self._rate_limit_ok():
            await self.send(text_data=json.dumps(
                {'tipo': 'log', 'mensagem': 'Muitos comandos. Aguarde um instante.'}
            ))
            return

        try:
            data = json.loads(text_data)
        except (json.JSONDecodeError, ValueError):
            return

        if not isinstance(data, dict):
            return

        validado = comando_sanitizado(data.get('comando', ''))
        if validado is None:
            await self.send(text_data=json.dumps(
                {'tipo': 'log', 'mensagem': 'Comando invalido.'}
            ))
            return

        verbo, arg = validado
        print(f"Comando aceito: {verbo} {arg}")

        if verbo == 'PLAY':
            if self.in_queue or self.java_socket:
                return
            self.in_queue = True
            await self.entrar_na_fila()
            return

        if not self.java_socket:
            await self.send(text_data=json.dumps(
                {'tipo': 'log', 'mensagem': 'Voce ainda nao esta em uma batalha.'}
            ))
            return

        comando_para_java = f"{verbo} {arg}\n".encode('utf-8')
        try:
            self.java_socket.sendall(comando_para_java)
        except Exception as e:
            print(f"Falha ao enviar para Java: {e}")

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

                try:
                    await self.conectar_java()
                    await opponent['ws'].conectar_java()
                except Exception as e:
                    print(f"Falha ao conectar no servidor Java: {e}")
                    err = json.dumps({'tipo': 'log',
                                      'mensagem': 'Servidor de batalha indisponivel.'})
                    await self.send(text_data=err)
                    await opponent['ws'].send(text_data=err)
                    return

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
        self.java_socket.settimeout(5)
        self.java_socket.connect((JAVA_HOST, JAVA_PORT))
        self.java_socket.sendall(f'AUTH {BATALHA_AUTH_TOKEN}\n'.encode('utf-8'))
        self.java_socket.settimeout(None)
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

_TEAM_BLOCKLIST = re.compile(r'[,;|\n\r\t]')


def _limpar(valor) -> str:
    return _TEAM_BLOCKLIST.sub('', str(valor)).strip()[:64]


def montar_team_str(team):

    pokemons = []
    for p in team:
        moves_limpos = []
        for m in p.get('moves', []):
            nome = _limpar(m.get('nome', ''))
            poder = int(m.get('poder', 0))
            tipo = _limpar(m.get('tipo', 'NORMAL'))
            if not nome or not tipo:
                continue
            moves_limpos.append(f"{nome}:{poder}:{tipo}")
        moves_str = '|'.join(moves_limpos)

        sprite_raw = p.get('sprite') or ''
        if sprite_raw.startswith(('http://', 'https://')):
            sprite = _TEAM_BLOCKLIST.sub('', sprite_raw).strip()
            if len(sprite) > 512:
                sprite = ''
        else:
            sprite = ''

        pokemons.append(
            f"{_limpar(p['nome'])},"
            f"{int(p['hp'])},{int(p['atk'])},{int(p['def'])},{int(p['spd'])},"
            f"{_limpar(p['tipo'])},{sprite},{moves_str}"
        )
    return ';'.join(pokemons)
