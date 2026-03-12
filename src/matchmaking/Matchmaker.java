package matchmaking;

import server.ClientHandler;
import battle.Battle;

public class Matchmaker {
	public void matchPlayers(ClientHandler player1, ClientHandler player2) {
        Battle battle = new Battle(player1, player2);
        player1.setBattle(battle);
        player2.setBattle(battle);
    }
}
