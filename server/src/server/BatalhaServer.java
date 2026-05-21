package server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import battle.Battle;

public class BatalhaServer {

    public static final String AUTH_TOKEN = System.getenv("BATALHA_AUTH_TOKEN");

    private static Queue<ClientHandler> matchmakingQueue = new ConcurrentLinkedQueue<>();

    public static synchronized void addPlayerToQueue(ClientHandler player) {
        if (matchmakingQueue.isEmpty()) {
            matchmakingQueue.add(player);
        } else {
            ClientHandler opponent = matchmakingQueue.poll();
            Battle battle = new Battle(player, opponent);
            player.setBattle(battle);
            opponent.setBattle(battle);
        }
    }

    public static void main(String[] args) {

        if (AUTH_TOKEN == null || AUTH_TOKEN.isEmpty()) {
            System.err.println("ERRO: variavel de ambiente BATALHA_AUTH_TOKEN nao definida.");
            System.err.println("Defina antes de iniciar o servidor, por exemplo:");
            System.err.println("  export BATALHA_AUTH_TOKEN=\"$(openssl rand -hex 32)\"");
            System.exit(1);
        }

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(
                port, 50, InetAddress.getByName("127.0.0.1"))) {

            System.out.println("Servidor iniciado em 127.0.0.1:" + port);

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo jogador conectado");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeFromQueue(ClientHandler player) {
        matchmakingQueue.remove(player);
    }
}
