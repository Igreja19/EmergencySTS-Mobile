package pt.ipleiria.estg.dei.emergencysts.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import pt.ipleiria.estg.dei.emergencysts.modelo.Paciente;

public class PacienteJsonParser {

    /**
     * Converte um objeto JSON num objeto Paciente
     */
    public static Paciente parserJsonPaciente(JSONObject json) {
        try {
            // Verifica se os dados estão dentro de "userprofile" ou na raiz
            JSONObject u = json.optJSONObject("userprofile");
            if (u == null) u = json;

            // Dados base do utilizador (se disponíveis)
            int id = json.optInt("id", -1);
            String username = json.optString("username", "---");
            String email = json.optString("email", "---");

            // Dados do perfil
            String nome = u.optString("nome", "Desconhecido");
            String dataNascimento = u.optString("datanascimento", "---");
            String telefone = u.optString("telefone", "---");
            String sns = u.optString("sns", "---");
            String nif = u.optString("nif", "---");
            String morada = u.optString("morada", "---");

            // Cria e devolve o objeto
            return new Paciente(id, username, email, "paciente", nome, dataNascimento, telefone, sns, nif, morada);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Caso a API devolva uma lista de pacientes
     */
    public static ArrayList<Paciente> parserJsonPacientes(JSONArray array) {
        ArrayList<Paciente> lista = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                Paciente p = parserJsonPaciente(array.getJSONObject(i));
                if (p != null) {
                    lista.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }
}