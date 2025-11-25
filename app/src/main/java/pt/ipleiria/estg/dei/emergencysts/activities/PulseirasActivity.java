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

        String url = baseUrl + "api/pulseira?status=Em%20espera&access-token=" + accessToken;

        System.out.println("URL PEDIDO: " + url);

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
                            Toast.makeText(this, "JSON sem chave 'data'", Toast.LENGTH_SHORT).show();
                        }

                        if (data != null) {
                            pulseiras.clear();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject pulseiraJson = data.getJSONObject(i);
                                String nomePaciente = "Desconhecido";

                                if (pulseiraJson.has("paciente") && !pulseiraJson.isNull("paciente")) {
                                    JSONObject pacienteJson = pulseiraJson.getJSONObject("paciente");
                                    nomePaciente = pacienteJson.getString("nome");
                                }

                                Pulseira pulseira = new Pulseira(
                                        pulseiraJson.optString("id"),
                                        pulseiraJson.optString("prioridade"),
                                        pulseiraJson.optString("status"),
                                        nomePaciente
                                );
                                pulseiras.add(pulseira);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    String erroMsg = error.getMessage();
                    if (error.networkResponse != null) {
                        erroMsg = "Erro: " + error.networkResponse.statusCode;
                    }
                    Toast.makeText(this, erroMsg, Toast.LENGTH_LONG).show();
                    System.out.println("VOLLEY ERROR: " + erroMsg);
                    progressBar.setVisibility(View.GONE);
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}
