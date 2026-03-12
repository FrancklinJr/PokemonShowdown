package model;
import java.util.*;

public class PokemonSpecies {

    private String name;

    private int baseHp;
    private int baseAttack;
    private int baseDefense;
    private int baseSpeed;

    private List<Move> moveset;

    public PokemonSpecies(
            String name,
            int baseHp,
            int baseAttack,
            int baseDefense,
            int baseSpeed,
            List<Move> moveset
    ) {

        this.name = name;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseSpeed = baseSpeed;
        this.moveset = moveset;
    }

    public String getName() {
        return name;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public int getBaseAttack() {
        return baseAttack;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public int getBaseSpeed() {
        return baseSpeed;
    }

    public List<Move> getMoveset() {
        return moveset;
    }
}