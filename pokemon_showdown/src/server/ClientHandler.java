package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
	
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	
	private ClientHandler opponent;
	
	private int hp = 100;
	private boolean myTurn = false;
	
	public ClientHandler(Socket socket) {
		this.socket = socket;
		
		try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public void setOpponent(ClientHandler opponent) {
		this.opponent = opponent;
	}
	
	public void sendMessage(String message) {
		out.println(message);
	}
	
	public void startBattle(boolean firstTurn) {

        this.myTurn = firstTurn;

        sendMessage("BATALHA INICIADA!");
        sendMessage("Seu HP: " + hp);

        if (firstTurn) {
            sendMessage("Seu turno!");
        } else {
            sendMessage("Turno do oponente!");
        }
    }
	
	public void run() {
		try {

			String mensagem;
			
			while ((mensagem = in.readLine()) != null) {
				
			    System.out.println("Mensagem recebida: " + mensagem);
				
				if(mensagem.equals("PLAY")) {
					System.out.println("Jogador entrou no matchmaking");
					BatalhaServer.addPlayerToQueue(this);
				}
				else if(mensagem.startsWith("MOVE")) {
					
					if(!myTurn) {
						sendMessage("Não é seu turno!");
						continue;
					}
					
					attack();
				}
			}
		} catch (Exception e) {
			System.out.println("Jogador desconectado.");
		}
	}
	
	private void attack() {
		
		int damage = 10;
		
		opponent.hp -= damage;
		
		sendMessage("Você atacou! Dano: " + damage);

        sendMessage("HP do oponente: " + opponent.hp);
        opponent.sendMessage("Seu HP: " + opponent.hp);

        if (opponent.hp <= 0) {

            sendMessage("Você venceu!");
            opponent.sendMessage("Você perdeu!");

            return;
        }

        myTurn = false;
        opponent.myTurn = true;

        opponent.sendMessage("Seu turno!");
        sendMessage("Turno do oponente!");
	}
}
