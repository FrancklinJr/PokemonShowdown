package battle;

import java.util.*;

import data.PokemonDatabase;
import model.Move;
import model.Pokemon;
import model.PokemonSpecies;
import server.ClientHandler;

public class Battle {

    private ClientHandler player1;
    private ClientHandler player2;

    private List<Pokemon> team1 = new ArrayList<>();
    private List<Pokemon> team2 = new ArrayList<>();

    private int active1 = 0;
    private int active2 = 0;

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

        player1.sendMessage("Seu Pokémon: " + p1.getName());
        player1.sendMessage("Oponente: " + p2.getName());

        player2.sendMessage("Seu Pokémon: " + p2.getName());
        player2.sendMessage("Oponente: " + p1.getName());
    }

    public void performMove(ClientHandler player, int moveIndex) {

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

        int damage = move.getPower() + attacker.getAttack() - defender.getDefense();

        if(damage < 1) {
            damage = 1;
        }

        defender.receiveDamage(damage);

        player1.sendMessage(attacker.getName() + " usou " + move.getName());
        player2.sendMessage(attacker.getName() + " usou " + move.getName());

        player1.sendMessage(defender.getName() + " tomou " + damage + " de dano");
        player2.sendMessage(defender.getName() + " tomou " + damage + " de dano");

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
}