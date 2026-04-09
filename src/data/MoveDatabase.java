package data;

import java.util.*;

import model.Move;
import model.Type;

public class MoveDatabase {

    private static Map<String, Move> moveMap = new HashMap<>();

    static {

    	add(new Move("Tackle",        40,  Type.NORMAL));
    	add(new Move("Quick Attack",  40,  Type.NORMAL));
    	add(new Move("Thunderbolt",   90,  Type.ELECTRIC));
    	add(new Move("Flamethrower",  90,  Type.FIRE));
    	add(new Move("Hydro Pump",    110, Type.WATER));
    	add(new Move("Solar Beam",    120, Type.GRASS));
    	add(new Move("Shadow Ball",   80,  Type.GHOST));
    	add(new Move("Hyper Beam",    150, Type.NORMAL));
        add(new Move("Knock Off",    65, Type.DARK));
        add(new Move("Body Slam",    85, Type.NORMAL));
        add(new Move("Giga Impact",    150, Type.NORMAL));
    }

    private static void add(Move move) {
        moveMap.put(move.getName(), move);
    }

    public static Move get(String name) {
        return moveMap.get(name);
    }
}
