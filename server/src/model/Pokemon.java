package model;

import java.util.*;

public class Pokemon {

    private PokemonSpecies species;

    private int maxHp;
    private int currentHp;

    private int attack;
    private int defense;
    private int speed;

    private List<Move> moves;

    public Pokemon(PokemonSpecies species) {

        this.species = species;

        this.maxHp = species.getBaseHp() * 3;
        this.currentHp = maxHp;

        this.attack = species.getBaseAttack();
        this.defense = species.getBaseDefense();
        this.speed = species.getBaseSpeed();

        this.moves = new ArrayList<>(species.getMoveset());
    }

    public String getName() {
        return species.getName();
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpeed() {
        return speed;
    }

    public int getCurrentHp() {
        return currentHp;
    }
    
    public int getMaxHp() {
        return maxHp;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void receiveDamage(int damage) {

        currentHp -= damage;

        if(currentHp < 0) {
            currentHp = 0;
        }
    }

    public boolean isFainted() {
        return currentHp <= 0;
    }

	public Type getType() {
		return species.getType();
	}
}