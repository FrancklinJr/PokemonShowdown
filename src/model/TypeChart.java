package model;

import java.util.HashMap;
import java.util.Map;

public class TypeChart {
	
	private static final Map<String, Double> chart = new HashMap<>();
	
	static {
        chart.put("FIRE_GRASS",    2.0);
        chart.put("FIRE_WATER",    0.5);
        chart.put("FIRE_FIRE",     0.5);

        chart.put("WATER_FIRE",    2.0);
        chart.put("WATER_GRASS",   0.5);
        chart.put("WATER_WATER",   0.5);

        chart.put("GRASS_WATER",   2.0);
        chart.put("GRASS_FIRE",    0.5);
        chart.put("GRASS_GRASS",   0.5);

        chart.put("ELECTRIC_WATER",  2.0);
        chart.put("ELECTRIC_GRASS",  0.5);
        chart.put("ELECTRIC_ELECTRIC", 0.5);
        
        chart.put("GHOST_PSYCHIC",  2.0);
        chart.put("GHOST_NORMAL",   0.0);
        chart.put("GHOST_GHOST",    2.0);

        chart.put("NORMAL_GHOST",   0.0);
    }

    public static double getMultiplier(Type attackType, Type defenderType) {
        String key = attackType.name() + "_" + defenderType.name();
        return chart.getOrDefault(key, 1.0);
    }
}
