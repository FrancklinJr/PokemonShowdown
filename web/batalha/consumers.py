import json
import socket
import asyncio
import threading
from channels.generic.websocket import AsyncWebsocketConsumer

JAVA_HOST = 'localhost'
JAVA_PORT = 5000

class BatalhaConsumer(AsyncWebsocketConsumer):

    async def connect(self):
        await self.accept()
        self.running = True
        # Salva o loop AQUI, na thread principal do asyncio
        self.loop = asyncio.get_running_loop()

        self.java_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.java_socket.connect((JAVA_HOST, JAVA_PORT))
        self.java_file = self.java_socket.makefile('r', encoding='utf-8')

        self.listener = threading.Thread(target=self._listen_java, daemon=True)
        self.listener.start()

        print("Jogador conectado.")

    async def disconnect(self, code):
        self.running = False
        try:
            self.java_socket.close()
        except:
            pass
        print("Jogador desconectado.")

    async def receive(self, text_data):
        data = json.loads(text_data)
        comando = data.get('comando', '')
        print(f"Comando: {comando}")
        self.java_socket.sendall((comando + '\n').encode('utf-8'))

    def _listen_java(self):
        try:
            for linha in self.java_file:
                linha = linha.strip()
                if linha and self.running:
                    # Usa o loop salvo no connect()
                    future = asyncio.run_coroutine_threadsafe(
                        self.send(text_data=linha),
                        self.loop
                    )
                    future.result(timeout=5)
        except Exception as e:
            print(f"Listener encerrado: {e}")