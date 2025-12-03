package pt.ipleiria.estg.dei.emergencysts.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;

public class PulseiraJsonParser {

    public static ArrayList<Pulseira> parserJsonPulseiras(JSONArray response) {
        ArrayList<Pulseira> lista = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject json = response.getJSONObject(i);
                Pulseira p = parserJsonPulseira(json);
                lista.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public static Pulseira parserJsonPulseira(JSONObject json) {
        // 1. Dados Básicos da Pulseira
        String id = json.optString("id");
        String prioridade = json.optString("prioridade");
        String status = json.optString("status");

        // Data/Hora (Tratamento de string)
        String rawDate = json.optString("tempoentrada");
        String horaFormatada = "--:--";
        if (rawDate.length() >= 16) {
            try {
                horaFormatada = rawDate.substring(11, 16);
            } catch (Exception e) {
                horaFormatada = "??:??";
            }
        }

        // 2. Dados do Paciente (userprofile)
        String nome = "Sem Nome";
        String sns = "---";
        String dataNasc = "--/--/----";
        String telefone = "---";

        JSONObject userProfile = json.optJSONObject("userprofile");
        // Fallback para APIs antigas
        if (userProfile == null) userProfile = json.optJSONObject("paciente");

        if (userProfile != null) {
            nome = userProfile.optString("nome");
            if (nome.isEmpty() || nome.equals("null")) nome = userProfile.optString("username", "Desconhecido");

            String tempSns = userProfile.optString("sns");
            if (tempSns != null && !tempSns.equals("null")) sns = tempSns;

            String tempData = userProfile.optString("datanascimento");
            if (tempData != null && !tempData.equals("null")) dataNasc = tempData;

            String tempTel = userProfile.optString("telefone");
            if (tempTel != null && !tempTel.equals("null")) telefone = tempTel;
        }

        // 3. Dados da Triagem
        String motivo = "-";
        String queixa = "-";
        String descricao = "-";
        String inicio = "-";
        String dor = "-";
        String alergias = "Não";
        String medicacao = "Nenhuma";

        JSONObject triagem = json.optJSONObject("triagem");
        if (triagem != null) {
            motivo = triagem.optString("motivoconsulta", "-");
            queixa = triagem.optString("queixaprincipal", "-");
            descricao = triagem.optString("descricaosintomas", "-");
            inicio = triagem.optString("iniciosintomas", "-");
            dor = triagem.optString("intensidadedor", "-");
            alergias = triagem.optString("alergias", "Não");
            medicacao = triagem.optString("medicacao", "Nenhuma");
        }

        // Retorna o objeto Pulseira preenchido com tudo
        return new Pulseira(id, prioridade, status, nome, sns, horaFormatada,
                dataNasc, telefone, motivo, queixa, descricao, inicio, dor, alergias, medicacao);
    }
}