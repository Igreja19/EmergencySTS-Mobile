package pt.ipleiria.estg.dei.emergencysts.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import pt.ipleiria.estg.dei.emergencysts.modelo.Triagem;

public class TriagemJsonParser {

    public static ArrayList<Triagem> parserJsonTriagens(JSONArray response) {
        ArrayList<Triagem> lista = new ArrayList<>();

        if (response == null) return lista;

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject obj = response.optJSONObject(i);
                if (obj != null) {
                    lista.add(parserJsonTriagem(obj));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static Triagem parserJsonTriagem(JSONObject json) {

        Triagem t = new Triagem();

        // TRIAGEM
        t.id = json.optInt("id");
        t.motivoconsulta    = safe(json, "motivoconsulta", "-");
        t.queixaprincipal   = safe(json, "queixaprincipal", "-");
        t.descricaosintomas = safe(json, "descricaosintomas", "-");
        t.iniciosintomas    = safe(json, "iniciosintomas", "-");
        t.alergias          = safe(json, "alergias", "-");
        t.medicacao         = safe(json, "medicacao", "-");
        t.datatriagem       = safe(json, "datatriagem", "");

        // USERPROFILE
        JSONObject up = json.optJSONObject("userprofile");
        t.userprofile = new Triagem.UserProfile();

        if (up != null) {
            t.userprofile.id    = up.optInt("id");
            t.userprofile.nome  = safe(up, "nome", "Sem nome");
            t.userprofile.email = safe(up, "email", "-");
            t.userprofile.sns   = safe(up, "sns", "---");
        } else {
            t.userprofile.nome = "Sem nome";
            t.userprofile.sns  = "---";
        }

        // PULSEIRA
        JSONObject p = json.optJSONObject("pulseira");
        t.pulseira = new Triagem.Pulseira();

        if (p != null) {
            t.pulseira.id           = p.optInt("id");
            t.pulseira.codigo       = safe(p, "codigo", "-");
            t.pulseira.prioridade   = safe(p, "prioridade", "Pendente");
            t.pulseira.status       = safe(p, "status", "Concluída");
            t.pulseira.tempoentrada = safe(p, "tempoentrada", "");
        } else {
            t.pulseira.codigo = "-";
            t.pulseira.prioridade = "Pendente";
            t.pulseira.status = "Concluída";
        }

        // CONSULTA
        JSONObject c = json.optJSONObject("consulta");
        t.consulta = new Triagem.Consulta();

        if (c != null) {
            t.consulta.id     = c.optInt("id");
            t.consulta.estado = safe(c, "estado", "Em curso");
        } else {
            t.consulta.estado = "Em curso";
        }

        return t;
    }

    private static String safe(JSONObject json, String key, String defaultValue) {
        if (json == null) return defaultValue;

        String value = json.optString(key, defaultValue);
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null"))
            return defaultValue;

        return value;
    }
}
