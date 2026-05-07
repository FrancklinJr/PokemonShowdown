package server;

import java.util.List;
import model.Move;
import model.Pokemon;

public class JsonMessage {

    private static String esc(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static String estado(
    		String seupokemon, int hp, int maxHp,
            int atk, int def, int spd, String tipo, String sprite,
            String opPokemon, int opHp, int opMaxHp, String opTipo, String opSprite,
            List<Move> moves, List<Pokemon> team, int activeIndex) {

        StringBuilder moves_json = new StringBuilder("[");
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            moves_json.append(String.format(
                "{\"nome\":\"%s\",\"poder\":%d,\"tipo\":\"%s\"}",
                esc(m.getName()), m.getPower(), m.getType().name()
            ));
            if (i < moves.size() - 1) moves_json.append(",");
        }
        moves_json.append("]");

        StringBuilder team_json = new StringBuilder("[");
        for (int i = 0; i < team.size(); i++) {
            Pokemon p = team.get(i);
            team_json.append(String.format(
                "{\"nome\":\"%s\",\"hp\":%d,\"maxHp\":%d,\"tipo\":\"%s\",\"ativo\":%b,\"desmaiado\":%b}",
                esc(p.getName()), p.getCurrentHp(), p.getMaxHp(),
                p.getType().name(), i == activeIndex, p.isFainted()
            ));
            if (i < team.size() - 1) team_json.append(",");
        }
        team_json.append("]");

        return String.format(
                "{\"tipo\":\"estado\"," +
                "\"seu_pokemon\":\"%s\",\"hp\":%d,\"maxHp\":%d," +
                "\"atk\":%d,\"def\":%d,\"spd\":%d,\"tipo_pokemon\":\"%s\",\"sprite\":\"%s\"," +
                "\"op_pokemon\":\"%s\",\"op_hp\":%d,\"op_maxHp\":%d,\"op_tipo\":\"%s\",\"op_sprite\":\"%s\"," +
                "\"moves\":%s,\"team\":%s}",
                esc(seupokemon), hp, maxHp, atk, def, spd, tipo, esc(sprite),
                esc(opPokemon), opHp, opMaxHp, opTipo, esc(opSprite),
                moves_json, team_json
            );
    }

    public static String log(String mensagem) {
        return String.format(
            "{\"tipo\":\"log\",\"mensagem\":\"%s\"}", esc(mensagem));
    }

    public static String aguardando() {
        return "{\"tipo\":\"aguardando\"}";
    }

    public static String fim(String resultado) {
        return String.format(
            "{\"tipo\":\"fim\",\"resultado\":\"%s\"}", resultado);
    }
}