package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import battle.Battle;
import model.Pokemon;

public class ClientHandler implements Runnable {

    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    private Battle battle;

    private List<Pokemon> team;
    private Pokemon currentPokemon;

    public ClientHandler(Socket socket) {

        this.socket = socket;

        try {

            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBattle(Battle battle) {
        this.battle = battle;
    }

    public void setTeam(List<Pokemon> team) {
        this.team = team;
    }

    public List<Pokemon> getTeam() {
        return team;
    }

    public void setCurrentPokemon(Pokemon pokemon) {
        this.currentPokemon = pokemon;
    }

    public Pokemon getCurrentPokemon() {
        return currentPokemon;
    }

    public void sendMessage(String message) {
        output.println(message);
    }

    @Override
    public void run() {

        try {

            sendMessage("Conectado ao servidor!");
            sendMessage("Digite PLAY para entrar na fila.");

            String line;

            while ((line = input.readLine()) != null) {

                System.out.println("Mensagem recebida: " + line);

                if (line.equalsIgnoreCase("PLAY")) {

                    sendMessage("Entrando na fila...");
                    BatalhaServer.addPlayerToQueue(this);

                } 
                
                else if (line.startsWith("MOVE")) {

                    if (battle != null) {

                        String moveName = line.substring(5);
                        battle.selectMove(this, moveName);

                    } else {

                        sendMessage("Você ainda não está em uma batalha!");
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("Jogador desconectado.");
        }
    }
}