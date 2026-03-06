package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class BatalhaServer {
	
	private static Queue<ClientHandler> matchmakingQueue = new LinkedList<>();
	
	public static synchronized void addPlayerToQueue(ClientHandler player) {
		
		if(matchmakingQueue.isEmpty()) {
			matchmakingQueue.add(player);
			player.sendMessage("Aguardando outro jogador...");
		}
		else {
			ClientHandler opponent = matchmakingQueue.poll();

            player.setOpponent(opponent);
            opponent.setOpponent(player);

            player.sendMessage("Partida encontrada!");
            opponent.sendMessage("Partida encontrada!");

            player.startBattle(true);
            opponent.startBattle(true);
		}
	}
	
	public static void main(String[] args) {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port);

            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo jogador conectado");

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
