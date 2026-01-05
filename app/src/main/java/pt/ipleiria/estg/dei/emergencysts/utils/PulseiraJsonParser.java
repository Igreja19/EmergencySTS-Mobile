package pt.ipleiria.estg.dei.emergencysts.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;

public class PulseiraJsonParser {

    /**
     * Método para fazer parse de uma LISTA de pulseiras (JSONArray)
     * Usado no MostrarPulseirasActivity
     */
    public static ArrayList<Pulseira> parserJsonPulseiras(JSONArray response) {
        ArrayList<Pulseira> pulseiras = new ArrayList<>();
        if (response == null) return pulseiras;

        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject obj = response.getJSONObject(i);
                Pulseira p = parserJsonPulseira(obj); // Reutiliza o método individual
                if (p != null) {
                    pulseiras.add(p);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return pulseiras;
    }

    /**
     * Método para fazer parse de UMA ÚNICA pulseira (JSONObject)
     * Usado no AtribuirPulseiraActivity e Detalhes
     */
    public static Pulseira parserJsonPulseira(JSONObject obj) {
        if (obj == null) return null;

        try {
            // 1. Campos diretos da Pulseira
            int id = obj.optInt("id");
            String codigo = obj.optString("codigo", "---");
            String prioridade = obj.optString("prioridade", "Pendente");
            String status = obj.optString("status", "Desconhecido");
            String dataEntrada = obj.optString("tempoentrada", ""); // ou 'hora'
            int userProfileId = obj.optInt("userprofile_id", -1);

            // 2. Dados do Paciente (UserProfile)
            String nome = "Anónimo";
            String sns = "";
            String dataNasc = "";
            String telefone = "";

            if (obj.has("userprofile") && !obj.isNull("userprofile")) {
                JSONObject user = obj.getJSONObject("userprofile");
                nome = user.optString("nome", "Sem Nome");
                sns = user.optString("sns", "");
                dataNasc = user.optString("data_nascimento", "");
                telefone = user.optString("telefone", "");
            }

            // 3. Dados da Triagem (Pode vir num objeto "triagem" ou na raiz)
            JSONObject triagemObj = obj; // Por defeito procura na raiz
            if (obj.has("triagem") && !obj.isNull("triagem")) {
                triagemObj = obj.getJSONObject("triagem");
            }

            String motivo = triagemObj.optString("motivo", "");
            String queixa = triagemObj.optString("queixa", "");
            String descricao = triagemObj.optString("descricao", "");
            String inicio = triagemObj.optString("inicio_sintomas", "");
            String dor = triagemObj.optString("escala_dor", "");
            String alergias = triagemObj.optString("alergias", "");
            String medicacao = triagemObj.optString("medicacao", "");

            // Retornar o objeto completo
            return new Pulseira(id, codigo, prioridade, status, dataEntrada,
                    userProfileId, nome, sns, dataNasc, telefone,
                    motivo, queixa, descricao, inicio, dor, alergias, medicacao);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Método utilitário para verificar internet
    public static boolean isConnectionInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }
}