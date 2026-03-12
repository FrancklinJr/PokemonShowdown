package data;

import java.util.*;

import model.Move;

public class MoveDatabase {

    private static Map<String, Move> moveMap = new HashMap<>();

    static {

        add(new Move("Tackle", 40));
        add(new Move("Quick Attack", 40));
        add(new Move("Thunderbolt", 90));
        add(new Move("Flamethrower", 90));
        add(new Move("Hydro Pump", 110));
        add(new Move("Solar Beam", 120));
        add(new Move("Shadow Ball", 80));
        add(new Move("Hyper Beam", 150));
    }

    private static void add(Move move) {
        moveMap.put(move.getName(), move);
    }

    public static Move get(String name) {
        return moveMap.get(name);
    }
}
