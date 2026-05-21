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
    private String teamData;
    private boolean authenticated = false;

    private long windowStart = 0L;
    private int commandsInWindow = 0;
    private static final int MAX_COMMANDS_PER_SECOND = 10;

    public String getTeamData() {
        return teamData;
    }

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

    private boolean checkRateLimit() {
        long now = System.currentTimeMillis();
        if (now - windowStart >= 1000L) {
            windowStart = now;
            commandsInWindow = 1;
            return true;
        }
        commandsInWindow++;
        return commandsInWindow <= MAX_COMMANDS_PER_SECOND;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = input.readLine()) != null) {

                if (line.length() > 4096) {
                    sendMessage(JsonMessage.log("Comando muito longo."));
                    break;
                }

                if (!checkRateLimit()) {
                    sendMessage(JsonMessage.log("Rate limit excedido."));
                    break;
                }

                if (!authenticated) {
                    if (line.startsWith("AUTH ")) {
                        String token = line.substring(5).trim();
                        if (constantTimeEquals(token, BatalhaServer.AUTH_TOKEN)) {
                            authenticated = true;
                            sendMessage(JsonMessage.log("Conectado ao servidor!"));
                            sendMessage(JsonMessage.log("Digite PLAY para entrar na fila."));
                            continue;
                        }
                    }
                    System.out.println("Conexao recusada: AUTH invalido.");
                    break;
                }

                System.out.println("Mensagem recebida: " + line);

                if (line.equalsIgnoreCase("PLAY")) {
                    sendMessage(JsonMessage.log("Entrando na fila..."));
                    BatalhaServer.addPlayerToQueue(this);
                } else if (line.startsWith("MOVE ")) {
                    if (battle != null) {
                        String moveName = line.substring(5).trim();
                        battle.selectMove(this, moveName);
                    } else {
                        sendMessage(JsonMessage.log("Você ainda não está em uma batalha!"));
                    }

                } else if (line.startsWith("SWITCH ")) {
                    if (battle != null) {
                        String pokemonName = line.substring(7).trim();
                        battle.switchPokemon(this, pokemonName);
                    } else {
                        sendMessage(JsonMessage.log("Você ainda não está em uma batalha!"));
                    }
                } else if (line.startsWith("TEAM ")) {
                    String teamData = line.substring(5).trim();
                    this.teamData = teamData;
                    BatalhaServer.addPlayerToQueue(this);
                }
            }

        } catch (Exception e) {
            System.out.println("Jogador desconectado.");
        } finally {
            handleDisconnect();
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private void handleDisconnect() {
        BatalhaServer.removeFromQueue(this);
        if (battle != null) {
            battle.handlePlayerDisconnect(this);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("Erro ao fechar socket: " + e.getMessage());
        }
    }
}
