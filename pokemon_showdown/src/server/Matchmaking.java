package server;

import java.util.LinkedList;
import java.util.Queue;

public class Matchmaking {
	private static Queue<ClientHandler> waitingPlayers = new LinkedList<>();
	
	public static synchronized void addPlayer(ClientHandler player) {
		
		waitingPlayers.add(player);
		
		System.out.println("Jogador na fila.");
		
		if (waitingPlayers.size() >= 2) {
			
			ClientHandler player1 = waitingPlayers.poll();
			ClientHandler player2 = waitingPlayers.poll();
			
			startBattle(player1, player2);
		}
	}

	private static void startBattle(ClientHandler p1, ClientHandler p2) {
		
		System.out.println("Batalha iniciada!");
		
		p1.sendMessage("Batalha iniciada contra um adversário!");
		p2.sendMessage("Batalha iniciada contra um adversário!");
		
	}
}
