package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import battle.Battle;

public class BatalhaServer {

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

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Servidor iniciado na porta " + port);

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