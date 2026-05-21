package data;

import model.*;
import java.util.*;

public class PokemonFactory {

    private static final int MAX_HP        = 300;
    private static final int MAX_ATK       = 200;
    private static final int MAX_DEF       = 200;
    private static final int MAX_SPD       = 200;
    private static final int MIN_STAT      = 1;
    private static final int MAX_MOVE_POW  = 200;
    private static final int MAX_TEAM_SIZE = 6;
    private static final int MAX_MOVES_PER_POKEMON = 4;
    private static final int MAX_NAME_LEN  = 32;

    public static List<Pokemon> fromTeamData(String teamData) {
        List<Pokemon> team = new ArrayList<>();
        if (teamData == null || teamData.isEmpty()) return team;

        String[] pokemons = teamData.split(";");

        for (String p : pokemons) {
            if (team.size() >= MAX_TEAM_SIZE) break;

            try {
                String[] parts = p.split(",");
                if (parts.length < 8) continue;

                String nome = sanitize(parts[0]);
                if (nome.isEmpty()) continue;

                int hp  = clamp(parseIntSafe(parts[1]), MIN_STAT, MAX_HP);
                int atk = clamp(parseIntSafe(parts[2]), MIN_STAT, MAX_ATK);
                int def = clamp(parseIntSafe(parts[3]), MIN_STAT, MAX_DEF);
                int spd = clamp(parseIntSafe(parts[4]), MIN_STAT, MAX_SPD);

                Type tipo = parseTypeSafe(parts[5]);
                String sprite = sanitizeSprite(parts[6]);
                String movesRaw = parts[7];

                List<Move> moves = new ArrayList<>();
                for (String m : movesRaw.split("\\|")) {
                    if (moves.size() >= MAX_MOVES_PER_POKEMON) break;
                    String[] mp = m.split(":");
                    if (mp.length != 3) continue;

                    String moveName = sanitize(mp[0]);
                    if (moveName.isEmpty()) continue;

                    int power = clamp(parseIntSafe(mp[1]), 0, MAX_MOVE_POW);
                    Type moveType = parseTypeSafe(mp[2]);

                    moves.add(new Move(moveName, power, moveType));
                }

                if (moves.isEmpty()) continue;

                PokemonSpecies species = new PokemonSpecies(nome, tipo, hp, atk, def, spd, moves);
                Pokemon pokemon = new Pokemon(species);
                pokemon.setSprite(sprite);
                team.add(pokemon);

            } catch (Exception e) {
                System.err.println("Pokemon ignorado por dados invalidos: " + e.getMessage());
            }
        }

        return team;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private static Type parseTypeSafe(String raw) {
        try {
            return Type.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return Type.NORMAL;
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        String cleaned = s.replaceAll("[\\p{Cntrl},;|]", "").trim();
        if (cleaned.length() > MAX_NAME_LEN) {
            cleaned = cleaned.substring(0, MAX_NAME_LEN);
        }
        return cleaned;
    }

    private static String sanitizeSprite(String s) {
        if (s == null) return "";
        String cleaned = s.replaceAll("[\\p{Cntrl},;|\"'<>]", "").trim();
        if (cleaned.isEmpty()) return "";
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            return "";
        }
        if (cleaned.length() > 512) return "";
        return cleaned;
    }
}
