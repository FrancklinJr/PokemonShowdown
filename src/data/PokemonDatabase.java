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

        add(new PokemonSpecies(
                "Regigigas",
                Type.NORMAL,
                110,
                160,
                110,
                100,
                Arrays.asList(
                        MoveDatabase.get("Giga impact"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Body Slam"),
                        MoveDatabase.get("Knock Off")
                )
        ));

        add(new PokemonSpecies(
                "Sirfetchd",
                Type.FIGHTING,
                62,
                135,
                95,
                65,
                Arrays.asList(
                        MoveDatabase.get("Giga impact"),
                        MoveDatabase.get("Tackle"),
                        MoveDatabase.get("Body Slam"),
                        MoveDatabase.get("Knock Off")
                )
        ));

        add(new PokemonSpecies(
                "Mimikyu",
                Type.GHOST,
                55,
                90,
                80,
                96,
                Arrays.asList(
                        MoveDatabase.get("Shadow Ball"),
                        MoveDatabase.get("Play Rough"),
                        MoveDatabase.get("Shadow Claw"),
                        MoveDatabase.get("Shadow Sneak")
                )
        ));

        add(new PokemonSpecies(
                "Alcremie",
                Type.FAIRY,
                65,
                60,
                75,
                64,
                Arrays.asList(
                        MoveDatabase.get("Alluring Voice"),
                        MoveDatabase.get("Play Rough"),
                        MoveDatabase.get("Draining Kiss"),
                        MoveDatabase.get("Dazzling Gleam")
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
