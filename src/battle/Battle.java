package battle;

import java.util.*;

import data.PokemonDatabase;
import model.Move;
import model.Pokemon;
import model.PokemonSpecies;
import model.TypeChart;
import server.ClientHandler;

public class Battle {

    private ClientHandler player1;
    private ClientHandler player2;

    private List<Pokemon> team1 = new ArrayList<>();
    private List<Pokemon> team2 = new ArrayList<>();

    private volatile int active1 = 0;
    private volatile int active2 = 0;
    
    private Integer moveP1 = null;
    private Integer moveP2 = null;

    public Battle(ClientHandler p1, ClientHandler p2) {

        this.player1 = p1;
        this.player2 = p2;

        generateTeam(team1);
        generateTeam(team2);

        startBattle();
    }

    private void generateTeam(List<Pokemon> team) {

        List<PokemonSpecies> pool = PokemonDatabase.getAll();

        Collections.shuffle(pool);

        for(int i = 0; i < 3; i++) {
            team.add(new Pokemon(pool.get(i)));
        }
    }

    private void startBattle() {

        player1.sendMessage("Batalha iniciada!");
        player2.sendMessage("Batalha iniciada!");

        sendActivePokemon();
    }

    private void sendActivePokemon() {

    	Pokemon p1 = team1.get(active1);
        Pokemon p2 = team2.get(active2);

        player1.sendMessage("=============================");
        player1.sendMessage("Seu Pokémon:  " + formatPokemon(p1));
        player1.sendMessage("Oponente:     " + formatPokemonEnemy(p2));
        player1.sendMessage("-----------------------------");
        player1.sendMessage(formatMoves(p1));
        player1.sendMessage("Digite: MOVE <nome do movimento>");
        player1.sendMessage("=============================");

        player2.sendMessage("=============================");
        player2.sendMessage("Seu Pokémon:  " + formatPokemon(p2));
        player2.sendMessage("Oponente:     " + formatPokemonEnemy(p1));
        player2.sendMessage("-----------------------------");
        player2.sendMessage(formatMoves(p2));
        player2.sendMessage("Digite: MOVE <nome do movimento>");
        player2.sendMessage("=============================");
        
        player1.sendMessage(formatTeam(team1, active1));
        player2.sendMessage(formatTeam(team2, active2));
    }

    private void performMove(ClientHandler player, int moveIndex) {

        Pokemon attacker;
        Pokemon defender;

        if(player == player1) {
            attacker = team1.get(active1);
            defender = team2.get(active2);
        } else {
            attacker = team2.get(active2);
            defender = team1.get(active1);
        }

        Move move = attacker.getMoves().get(moveIndex);

        int damage = calculateDamage(attacker, defender, move);

        defender.receiveDamage(damage);

        String attackMsg = String.format("⚡ %s usou %s! → %s tomou %d de dano (HP: %d/%d)",
        	    attacker.getName(),
        	    move.getName(),
        	    defender.getName(),
        	    damage,
        	    defender.getCurrentHp(),
        	    defender.getMaxHp()
        	);

        player1.sendMessage(attackMsg);
        player2.sendMessage(attackMsg);
        
        
        double effectiveness = TypeChart.getMultiplier(move.getType(), defender.getType());
        String effectMsg = "";

        if      (effectiveness == 0.0) effectMsg = "Não afeta " + defender.getName() + "...";
        else if (effectiveness >= 2.0) effectMsg = "É super efetivo!";
        else if (effectiveness <= 0.5) effectMsg = "Não é muito efetivo...";

        if (!effectMsg.isEmpty()) {
            player1.sendMessage(effectMsg);
            player2.sendMessage(effectMsg);
        }

        if(defender.isFainted()) {

            player1.sendMessage(defender.getName() + " desmaiou!");
            player2.sendMessage(defender.getName() + " desmaiou!");

            if(player == player1) {
                active2++;
            } else {
                active1++;
            }

            if(active1 >= team1.size() || active2 >= team2.size()) {
                endBattle(player);
                return;
            }

            sendActivePokemon();
        }
    }

    private void endBattle(ClientHandler winner) {

        ClientHandler loser = (winner == player1) ? player2 : player1;

        winner.sendMessage("Você venceu!");
        loser.sendMessage("Você perdeu!");
    }
    
    public synchronized void selectMove(ClientHandler player, String moveName) {

        int moveIndex = resolveMoveName(player, moveName);
        
        if (moveIndex == -1) {
        	player.sendMessage("Movimento inválido!");
        	return;
        }
        
        if (player == player1) {
            if (moveP1 != null) {
                player.sendMessage("Você já escolheu um movimento neste turno!");
                return;
            }
            moveP1 = moveIndex;
            player.sendMessage("Movimento selecionado! Aguardando oponente...");
        } else {
            if (moveP2 != null) {
                player.sendMessage("Você já escolheu um movimento neste turno!");
                return;
            }
            moveP2 = moveIndex;
            player.sendMessage("Movimento selecionado! Aguardando oponente...");
        }

        if (moveP1 != null && moveP2 != null) {
            try {
                resolveTurn();
            } finally {
            	moveP1 = null;
                moveP2 = null;
            }
        }
    }
    
    public synchronized void switchPokemon(ClientHandler player, String pokemonName) {

        List<Pokemon> team = (player == player1) ? team1 : team2;
        int activeIndex   = (player == player1) ? active1 : active2;

        int targetIndex = -1;
        for (int i = 0; i < team.size(); i++) {
            if (team.get(i).getName().equalsIgnoreCase(pokemonName)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            player.sendMessage("Pokémon não encontrado no seu time!");
            return;
        }

        if (targetIndex == activeIndex) {
            player.sendMessage(pokemonName + " já está em batalha!");
            return;
        }

        if (team.get(targetIndex).isFainted()) {
            player.sendMessage(pokemonName + " está desmaiado e não pode batalhar!");
            return;
        }

        if (player == player1) active1 = targetIndex;
        else                    active2 = targetIndex;

        Pokemon newPokemon = team.get(targetIndex);

        player1.sendMessage(player == player1
            ? "Vai, " + newPokemon.getName() + "!"
            : "Oponente trocou para " + newPokemon.getName() + "!");

        player2.sendMessage(player == player2
            ? "Vai, " + newPokemon.getName() + "!"
            : "Oponente trocou para " + newPokemon.getName() + "!");

        if (player == player1 && moveP2 != null) {
            resolveTurnAfterSwitch(player2, moveP2);
            moveP1 = null;
            moveP2 = null;
        } else if (player == player2 && moveP1 != null) {
            resolveTurnAfterSwitch(player1, moveP1);
            moveP1 = null;
            moveP2 = null;
        }

        sendActivePokemon();
    }
    
    private int resolveMoveName(ClientHandler player, String moveName) {

        Pokemon pokemon = (player == player1)
            ? team1.get(active1)
            : team2.get(active2);

        for (int i = 0; i < pokemon.getMoves().size(); i++) {
            if (pokemon.getMoves().get(i).getName().equalsIgnoreCase(moveName)) {
                return i;
            }
        }
        return -1;
    }
    
    private void resolveTurn() {

        Pokemon p1 = team1.get(active1);
        Pokemon p2 = team2.get(active2);

        boolean p1First = p1.getSpeed() >= p2.getSpeed();

        if (p1First) {
            performMove(player1, moveP1);

            if (!p2.isFainted()) {
                performMove(player2, moveP2);
            }
        } else {
            performMove(player2, moveP2);

            if (!p1.isFainted()) {
                performMove(player1, moveP1);
            }
        }
    }
    
    public synchronized void handlePlayerDisconnect(ClientHandler disconnected) {

        ClientHandler remaining = (disconnected == player1) ? player2 : player1;

        remaining.sendMessage("Seu oponente desconectou. Você venceu por W.O.!");

        moveP1 = null;
        moveP2 = null;
    }
    
    private String formatPokemon(Pokemon p) {
        return String.format("%-12s HP: %d/%d  ATK:%d  DEF:%d  SPD:%d",
            p.getName(),
            p.getCurrentHp(),
            p.getMaxHp(),
            p.getAttack(),
            p.getDefense(),
            p.getSpeed()
        );
    }
    
    private String formatPokemonEnemy(Pokemon p) {
        return String.format("%-12s HP: %d/%d",
            p.getName(),
            p.getCurrentHp(),
            p.getMaxHp()
        );
    }
    
    private String formatMoves(Pokemon p) {
        StringBuilder sb = new StringBuilder("Moves disponíveis:\n");
        List<Move> moves = p.getMoves();
        for (int i = 0; i < moves.size(); i++) {
            sb.append(String.format("  [%d] %-15s POW: %d\n",
                i + 1,
                moves.get(i).getName(),
                moves.get(i).getPower()
            ));
        }
        return sb.toString();
    }
    
    private int calculateDamage(Pokemon attacker, Pokemon defender, Move move) {
    	
    	double effectiveness = TypeChart.getMultiplier(
    	        move.getType(), 
    	        defender.getType()
    	    );

    	double stab = (move.getType() == attacker.getType()) ? 1.5 : 1.0;

        double base = ((double) attacker.getAttack() / defender.getDefense())
                      * move.getPower()
                      * 0.1
                      * effectiveness
                      * stab;

        double randomFactor = 0.85 + Math.random() * 0.15;

        int damage = (int) (base * randomFactor);

        return Math.max(effectiveness == 0.0 ? 0 : 1, damage);
    }
    
    private void resolveTurnAfterSwitch(ClientHandler attacker, int moveIndex) {

        player1.sendMessage("--- O oponente já havia escolhido um movimento! ---");
        player2.sendMessage("--- O oponente já havia escolhido um movimento! ---");

        performMove(attacker, moveIndex);
    }
    
    private String formatTeam(List<Pokemon> team, int activeIndex) {

        StringBuilder sb = new StringBuilder("Seu time:\n");

        for (int i = 0; i < team.size(); i++) {
            Pokemon p = team.get(i);
            String status;

            if (i == activeIndex)    status = "  ◄ em batalha";
            else if (p.isFainted())  status = "  ✗ desmaiado";
            else                     status = String.format("  HP: %d/%d", p.getCurrentHp(), p.getMaxHp());

            sb.append(String.format("  %-12s [%s]%s\n", p.getName(), p.getType(), status));
        }

        sb.append("Para trocar: SWITCH <nome do pokémon>");
        return sb.toString();
    }
}