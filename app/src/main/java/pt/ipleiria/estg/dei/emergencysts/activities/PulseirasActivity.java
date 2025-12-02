package pt.ipleiria.estg.dei.emergencysts.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.adapters.PulseiraAdapter;
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PulseirasActivity extends AppCompatActivity {

    private ListView listViewPulseiras;
    private ProgressBar progressBar;
    private PulseiraAdapter adapter;
    private ArrayList<Pulseira> pulseiras = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulseiras);

        listViewPulseiras = findViewById(R.id.listViewPulseiras);
        progressBar = findViewById(R.id.progressBar);

        adapter = new PulseiraAdapter(this, pulseiras);
        listViewPulseiras.setAdapter(adapter);

        getPulseiras();
    }

    private void getPulseiras() {
        progressBar.setVisibility(View.VISIBLE);

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String accessToken = SharedPrefManager.getInstance(this).getKeyAccessToken();

        // Garante que o URL está correto (ajusta se a tua API usar outro endpoint)
        String url = baseUrl + "api/pulseira?status=Em%20espera&prioridade=Pendente&access-token=" + accessToken;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONArray data = null;
                        if (response.has("data")) {
                            data = response.getJSONArray("data");
                        } else {
                            // Se a API retornar a lista diretamente sem "data"
                            // data = response; // (depende da estrutura exata do JSON)
                        }

                        if (data != null) {
                            pulseiras.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject pulseiraJson = data.getJSONObject(i);

                                String nomePaciente = "Sem Nome";
                                String snsPaciente = "---";

                                // 1. Buscar dados do Paciente (Objeto aninhado)
                                if (pulseiraJson.has("paciente") && !pulseiraJson.isNull("paciente")) {
                                    JSONObject pacienteJson = pulseiraJson.getJSONObject("paciente");

                                    // CORREÇÃO: Usa os nomes exatos da tua base de dados
                                    nomePaciente = pacienteJson.optString("nome", "Desconhecido");

                                    // Tenta buscar "sns". Se vier null ou "null", fica "---"
                                    String tempSns = pacienteJson.optString("sns");
                                    if (tempSns != null && !tempSns.equals("null") && !tempSns.isEmpty()) {
                                        snsPaciente = tempSns;
                                    }
                                }

                                // 2. Tentar obter a Hora da Pulseira
                                // Procura por campos comuns de data. Ajusta "created_at" ou "data_criacao" conforme a tua API.
                                String rawDate = pulseiraJson.optString("tempoentrada");
                                if (rawDate.isEmpty()) rawDate = pulseiraJson.optString("data_criacao");

                                String horaFormatada = "--:--";
                                if (!rawDate.isEmpty()) {
                                    // Pequena lógica para extrair apenas a hora (HH:mm) da string de data
                                    try {
                                        // Assume formato ISO "YYYY-MM-DD HH:mm:ss"
                                        if (rawDate.length() >= 16) {
                                            horaFormatada = rawDate.substring(11, 16);
                                        }
                                    } catch (Exception e) {
                                        horaFormatada = "??:??";
                                    }
                                } else {
                                    // Se não houver data na API, usamos a hora atual do sistema como fallback
                                    android.icu.text.SimpleDateFormat sdf = new android.icu.text.SimpleDateFormat("HH:mm");
                                    horaFormatada = sdf.format(new java.util.Date());
                                }

                                // 3. Criar o objeto com DADOS REAIS
                                Pulseira pulseira = new Pulseira(
                                        pulseiraJson.optString("id"),
                                        pulseiraJson.optString("prioridade"),
                                        pulseiraJson.optString("status"),
                                        nomePaciente,
                                        snsPaciente,     // Agora passa o SNS real
                                        horaFormatada    // Agora passa a hora
                                );
                                pulseiras.add(pulseira);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar dados", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    Toast.makeText(this, "Erro de rede: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}
