package data;

import java.util.*;

import model.PokemonSpecies;
import model.Type;

public class PokemonDatabase {

    private static Map<String, PokemonSpecies> pokemonMap = new HashMap<>();

    static {

        add(new PokemonSpecies(
                "Pikachu",
                Type.ELECTRIC,
                35,
                55,
                40,
                90,
                Arrays.asList(
                        MoveDatabase.get("Thunderbolt"),
                        MoveDatabase.get("Quick Attack"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Hyper Beam")
                )
        ));

        add(new PokemonSpecies(
                "Charizard",
                Type.FIRE,
                78,
                84,
                78,
                100,
                Arrays.asList(
                        MoveDatabase.get("Flamethrower"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Hyper Beam"),
                        MoveDatabase.get("Quick Attack")
                )
        ));

        add(new PokemonSpecies(
                "Blastoise",
                Type.WATER,
                79,
                83,
                100,
                78,
                Arrays.asList(
                        MoveDatabase.get("Hydro Pump"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Quick Attack"),
                        MoveDatabase.get("Hyper Beam")
                )
        ));

        add(new PokemonSpecies(
                "Venusaur",
                Type.GRASS,
                80,
                82,
                83,
                80,
                Arrays.asList(
                        MoveDatabase.get("Solar Beam"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Quick Attack"),
                        MoveDatabase.get("Hyper Beam")
                )
        ));

        add(new PokemonSpecies(
                "Gengar",
                Type.GHOST,
                60,
                65,
                60,
                110,
                Arrays.asList(
                        MoveDatabase.get("Shadow Ball"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Quick Attack"),
                        MoveDatabase.get("Hyper Beam")
                )
        ));
    }

    private static void add(PokemonSpecies species) {
        pokemonMap.put(species.getName(), species);
    }

    public static PokemonSpecies get(String name) {
        return pokemonMap.get(name);
    }

    public static List<PokemonSpecies> getAll() {
        return new ArrayList<>(pokemonMap.values());
    }
}