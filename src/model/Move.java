package model;

public class Move {

    private String name;
    private int power;
    private Type type;

    public Move(String name, int power, Type type) {
        this.name = name;
        this.power = power;
        this.type = type;
    }

    public String getName()  { return name; }
    public int getPower()    { return power; }
    public Type getType()    { return type; }
}