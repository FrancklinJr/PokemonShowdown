package battle;

import java.util.*;

import data.PokemonDatabase;
import data.PokemonFactory;
import model.Move;
import model.Pokemon;
import model.PokemonSpecies;
import model.Type;
import model.TypeChart;
import server.ClientHandler;
import server.JsonMessage;

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
        this.team1 = PokemonFactory.fromTeamData(p1.getTeamData());
        this.team2 = PokemonFactory.fromTeamData(p2.getTeamData());
        startBattle();
    }

    private void generateTeam(List<Pokemon> team) {
        List<PokemonSpecies> pool = PokemonDatabase.getAll();
        Collections.shuffle(pool);
        for (int i = 0; i < 3; i++) {
            team.add(new Pokemon(pool.get(i)));
        }
    }

    private void startBattle() {
        player1.sendMessage(JsonMessage.log("Batalha iniciada!"));
        player2.sendMessage(JsonMessage.log("Batalha iniciada!"));
        sendActivePokemon();
    }

    private void sendActivePokemon() {
    	Pokemon p1 = team1.get(active1);
        Pokemon p2 = team2.get(active2);

        player1.sendMessage(JsonMessage.estado(
        	    p1.getName(), p1.getCurrentHp(), p1.getMaxHp(),
        	    p1.getAttack(), p1.getDefense(), p1.getSpeed(),
        	    p1.getType().name(), p1.getSprite(),
        	    p2.getName(), p2.getCurrentHp(), p2.getMaxHp(),
        	    p2.getType().name(), p2.getSprite(),
        	    p1.getMoves(), team1, active1
        	));


        player2.sendMessage(JsonMessage.estado(
        	    p2.getName(), p2.getCurrentHp(), p2.getMaxHp(),
        	    p2.getAttack(), p2.getDefense(), p2.getSpeed(),
        	    p2.getType().name(), p2.getSprite(),
        	    p1.getName(), p1.getCurrentHp(), p1.getMaxHp(),
        	    p1.getType().name(), p1.getSprite(),
        	    p2.getMoves(),
        	    team2, active2
        	));

    }

    private void performMove(ClientHandler player, int moveIndex) {
    	System.out.println("DEBUG performMove: início, moveIndex=" + moveIndex);
    	
        Pokemon attacker;
        Pokemon defender;

        if (player == player1) {
            attacker = team1.get(active1);
            defender = team2.get(active2);
        } else {
            attacker = team2.get(active2);
            defender = team1.get(active1);
        }
        
        System.out.println("DEBUG performMove: " + attacker.getName() + " → " + defender.getName());

        Move move = attacker.getMoves().get(moveIndex);
        int damage = calculateDamage(attacker, defender, move);
        
        System.out.println("DEBUG performMove: dano=" + damage);

        defender.receiveDamage(damage);

        String attackMsg = attacker.getName() + " usou " + move.getName() +
            "! → " + defender.getName() + " tomou " + damage +
            " de dano (HP: " + defender.getCurrentHp() + "/" + defender.getMaxHp() + ")";
        
        System.out.println("DEBUG performMove: enviando log");
        player1.sendMessage(JsonMessage.log(attackMsg));
        player2.sendMessage(JsonMessage.log(attackMsg));
        
        System.out.println("DEBUG performMove: verificando efetividade");
        double effectiveness = TypeChart.getMultiplier(move.getType(), defender.getType());
        String effectMsg = "";
        if      (effectiveness == 0.0) effectMsg = "Não afeta " + defender.getName() + "...";
        else if (effectiveness >= 2.0) effectMsg = "É super efetivo!";
        else if (effectiveness <= 0.5) effectMsg = "Não é muito efetivo...";

        if (!effectMsg.isEmpty()) {
            player1.sendMessage(JsonMessage.log(effectMsg));
            player2.sendMessage(JsonMessage.log(effectMsg));
        }
        
        System.out.println("DEBUG performMove: verificando fainted");
        if (defender.isFainted()) {
        	System.out.println("DEBUG performMove: desmaiou!");
            player1.sendMessage(JsonMessage.log(defender.getName() + " desmaiou!"));
            player2.sendMessage(JsonMessage.log(defender.getName() + " desmaiou!"));

            if (player == player1) active2++;
            else                   active1++;

            if (active1 >= team1.size() || active2 >= team2.size()) {
                endBattle(player);
                return;
            }

            sendActivePokemon();
            System.out.println("DEBUG performMove: fim");
        }
    }

    private int calculateDamage(Pokemon attacker, Pokemon defender, Move move) {
        double effectiveness = TypeChart.getMultiplier(
            move.getType(), defender.getType());

        double stab = (move.getType() == attacker.getType()) ? 1.5 : 1.0;

        double base = ((double) attacker.getAttack() / defender.getDefense())
                      * move.getPower() * 0.1 * effectiveness * stab;

        double random = 0.85 + Math.random() * 0.15;

        int damage = (int)(base * random);
        return Math.max(effectiveness == 0.0 ? 0 : 1, damage);
    }

    private void endBattle(ClientHandler winner) {
        ClientHandler loser = (winner == player1) ? player2 : player1;
        winner.sendMessage(JsonMessage.fim("vitoria"));
        loser.sendMessage(JsonMessage.fim("derrota"));
    }

    public synchronized void selectMove(ClientHandler player, String moveName) {
        int moveIndex = resolveMoveName(player, moveName);

        if (moveIndex == -1) {
            player.sendMessage(JsonMessage.log("Movimento inválido!"));
            return;
        }

        if (player == player1) {
            if (moveP1 != null) {
                player.sendMessage(JsonMessage.log("Você já escolheu um movimento neste turno!"));
                return;
            }
            moveP1 = moveIndex;
        } else {
            if (moveP2 != null) {
                player.sendMessage(JsonMessage.log("Você já escolheu um movimento neste turno!"));
                return;
            }
            moveP2 = moveIndex;
        }

        player.sendMessage(JsonMessage.aguardando());

        if (moveP1 != null && moveP2 != null) {
            try {
                resolveTurn();
                if (active1 < team1.size() && active2 < team2.size()) {
                    sendActivePokemon();
                }
            } finally {
                moveP1 = null;
                moveP2 = null;
            }
        }
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
    	if (moveP1 == -1) {
            resolveTurnAfterSwitch(player2, moveP2);
            return;
        }
        if (moveP2 == -1) {
            resolveTurnAfterSwitch(player1, moveP1);
            return;
        }

        Pokemon p1 = team1.get(active1);
        Pokemon p2 = team2.get(active2);

        boolean p1First = p1.getSpeed() >= p2.getSpeed();

        if (p1First) {
            performMove(player1, moveP1);
            if (!p2.isFainted()) performMove(player2, moveP2);
        } else {
            performMove(player2, moveP2);
            if (!p1.isFainted()) performMove(player1, moveP1);
        }
    }

    private void resolveTurnAfterSwitch(ClientHandler attacker, int moveIndex) {
        player1.sendMessage(JsonMessage.log("O oponente já havia escolhido um movimento!"));
        player2.sendMessage(JsonMessage.log("O oponente já havia escolhido um movimento!"));
        performMove(attacker, moveIndex);
    }

    public synchronized void switchPokemon(ClientHandler player, String pokemonName) {
        List<Pokemon> team = (player == player1) ? team1 : team2;
        int activeIndex = (player == player1) ? active1 : active2;

        int targetIndex = -1;
        for (int i = 0; i < team.size(); i++) {
            if (team.get(i).getName().equalsIgnoreCase(pokemonName)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            player.sendMessage(JsonMessage.log("Pokemon nao encontrado no seu time!"));
            return;
        }
        if (targetIndex == activeIndex) {
            player.sendMessage(JsonMessage.log(pokemonName + " ja esta em batalha!"));
            return;
        }
        if (team.get(targetIndex).isFainted()) {
            player.sendMessage(JsonMessage.log(pokemonName + " esta desmaiado!"));
            return;
        }

        if (player == player1) active1 = targetIndex;
        else                   active2 = targetIndex;

        Pokemon newPokemon = team.get(targetIndex);

        player1.sendMessage(JsonMessage.log(player == player1
            ? "Vai, " + newPokemon.getName() + "!"
            : "Oponente trocou para " + newPokemon.getName() + "!"));
        player2.sendMessage(JsonMessage.log(player == player2
            ? "Vai, " + newPokemon.getName() + "!"
            : "Oponente trocou para " + newPokemon.getName() + "!"));

        if (player == player1 && moveP2 != null) {
            if (moveP2 == -1) {
                moveP1 = null;
                moveP2 = null;
                if (active1 < team1.size() && active2 < team2.size()) {
                    sendActivePokemon();
                }
            } else {
                resolveTurnAfterSwitch(player2, moveP2);
                moveP1 = null;
                moveP2 = null;
                if (active1 < team1.size() && active2 < team2.size()) {
                    sendActivePokemon();
                }
            }
        } else if (player == player2 && moveP1 != null) {
            if (moveP1 == -1) {
                moveP1 = null;
                moveP2 = null;
                if (active1 < team1.size() && active2 < team2.size()) {
                    sendActivePokemon();
                }
            } else {
                resolveTurnAfterSwitch(player1, moveP1);
                moveP1 = null;
                moveP2 = null;
                if (active1 < team1.size() && active2 < team2.size()) {
                    sendActivePokemon();
                }
            }
        } else {
            if (player == player1) moveP1 = -1;
            else                   moveP2 = -1;

            sendActivePokemon();
            player.sendMessage(JsonMessage.aguardando());
        }
    }

    public synchronized void handlePlayerDisconnect(ClientHandler disconnected) {
        ClientHandler remaining = (disconnected == player1) ? player2 : player1;
        remaining.sendMessage(JsonMessage.fim("wo"));
        moveP1 = null;
        moveP2 = null;
    }
}