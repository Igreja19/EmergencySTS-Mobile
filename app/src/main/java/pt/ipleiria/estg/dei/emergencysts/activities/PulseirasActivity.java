package pt.ipleiria.estg.dei.emergencysts.activities;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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

        ImageView btnback = findViewById(R.id.btnBack);
        btnback.setOnClickListener(v -> finish());

        listViewPulseiras = findViewById(R.id.listViewPulseiras);
        progressBar = findViewById(R.id.progressBar);

        adapter = new PulseiraAdapter(this, pulseiras);
        listViewPulseiras.setAdapter(adapter);

        listViewPulseiras.setOnItemClickListener((parent, view, position, id) -> {
            Pulseira pulseiraSelecionada = pulseiras.get(position);

            Intent intent = new Intent(PulseirasActivity.this, AtribuirPulseiraActivity.class);
            // Passa o ID para a nova activity saber qual carregar
            intent.putExtra("pulseira_id", pulseiraSelecionada.getId());

            startActivity(intent);
        });

    }

    protected void onResume() {
        super.onResume();
        getPulseiras();
    }

    private void getPulseiras() {
        progressBar.setVisibility(View.VISIBLE);

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String accessToken = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "api/pulseira?status=Em%20espera&prioridade=Pendente&expand=userprofile&access-token=" + accessToken;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        //  LER A CAIXA "DATA"
                        JSONArray data = null;
                        if (response.has("data")) {
                            data = response.getJSONArray("data");
                        } else {
                            data = response.optJSONArray("items");
                        }

                        if (data != null) {
                            pulseiras.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject pulseiraJson = data.getJSONObject(i);

                                String nomePaciente = "Sem Nome";
                                String snsPaciente = "---";

                                JSONObject userProfile = pulseiraJson.optJSONObject("userprofile");

                                if (userProfile != null) {
                                    // Tenta ler o 'nome', se não tiver, tenta 'username'
                                    nomePaciente = userProfile.optString("nome");
                                    if (nomePaciente.isEmpty()) nomePaciente = userProfile.optString("username", "Desconhecido");

                                    String tempSns = userProfile.optString("sns");
                                    if (tempSns != null && !tempSns.equals("null") && !tempSns.isEmpty()) {
                                        snsPaciente = tempSns;
                                    }
                                }

                                // DATA/HORA
                                String rawDate = pulseiraJson.optString("tempoentrada");
                                String horaFormatada = "--:--";
                                if (!rawDate.isEmpty() && rawDate.length() >= 16) {
                                    horaFormatada = rawDate.substring(11, 16);
                                }

                                //  CRIAR OBJETO
                                Pulseira pulseira = new Pulseira(
                                        pulseiraJson.optString("id"),
                                        pulseiraJson.optString("prioridade"),
                                        pulseiraJson.optString("status"),
                                        nomePaciente,
                                        snsPaciente,
                                        horaFormatada
                                );
                                pulseiras.add(pulseira);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar lista", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    Toast.makeText(this, "Erro de rede", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}
