package data;

import model.*;
import java.util.*;

public class PokemonFactory {

	public static List<Pokemon> fromTeamData(String teamData) {
        List<Pokemon> team = new ArrayList<>();
        String[] pokemons = teamData.split(";");

        for (String p : pokemons) {
            String[] parts = p.split(",");
            if (parts.length < 8) continue;

            String nome    = parts[0];
            int hp         = Integer.parseInt(parts[1]);
            int atk        = Integer.parseInt(parts[2]);
            int def        = Integer.parseInt(parts[3]);
            int spd        = Integer.parseInt(parts[4]);
            Type tipo      = Type.valueOf(parts[5]);
            String sprite  = parts[6];
            String movesRaw = parts[7];

            List<Move> moves = new ArrayList<>();
            for (String m : movesRaw.split("\\|")) {
                String[] mp = m.split(":");
                if (mp.length == 3) {
                    moves.add(new Move(mp[0], Integer.parseInt(mp[1]), Type.valueOf(mp[2])));
                }
            }

            PokemonSpecies species = new PokemonSpecies(nome, tipo, hp, atk, def, spd, moves);
            Pokemon pokemon = new Pokemon(species);
            pokemon.setSprite(sprite);
            team.add(pokemon);
        }

        return team;
    }
}
