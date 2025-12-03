package pt.ipleiria.estg.dei.emergencysts.activities;

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

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.adapters.PulseiraAdapter;
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraBDHelper;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraJsonParser;
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

        // Configuração da UI
        ImageView btnback = findViewById(R.id.btnBack);
        btnback.setOnClickListener(v -> finish());

        listViewPulseiras = findViewById(R.id.listViewPulseiras);
        progressBar = findViewById(R.id.progressBar);

        adapter = new PulseiraAdapter(this, pulseiras);
        listViewPulseiras.setAdapter(adapter);

        // Clique num item da lista -> Vai para AtribuirPulseira
        listViewPulseiras.setOnItemClickListener((parent, view, position, id) -> {
            Pulseira pulseiraSelecionada = pulseiras.get(position);
            Intent intent = new Intent(PulseirasActivity.this, AtribuirPulseiraActivity.class);
            intent.putExtra("pulseira_id", pulseiraSelecionada.getId());
            startActivity(intent);
        });
    }

    @Override
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
                    // --- SUCESSO (TEM INTERNET) ---
                    try {
                        // Lógica do envelope "data" (Caso a API devolva { "data": [...] })
                        JSONArray data = null;
                        if (response.has("data")) {
                            data = response.getJSONArray("data");
                        } else {
                            // Tenta buscar "items" ou assume que é o próprio response se fosse array (mas aqui é JsonObjectRequest)
                            data = response.optJSONArray("items");
                        }
                        if (data != null) {

                            //  USAR O PARSER
                            ArrayList<Pulseira> novasPulseiras = PulseiraJsonParser.parserJsonPulseiras(data);

                            // Atualizar a lista visual
                            pulseiras.clear();
                            pulseiras.addAll(novasPulseiras);
                            adapter.notifyDataSetChanged();

                            // Apaga o que estava lá e mete a lista nova
                            PulseiraBDHelper db = PulseiraBDHelper.getInstance(this);
                            db.removeAllPulseiras();
                            for (Pulseira p : novasPulseiras) {
                                db.adicionarPulseira(p);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar dados da API", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    // --- ERRO (MODO OFFLINE) ---
                    progressBar.setVisibility(View.GONE);
                    // Se a net falhar, carregamos do SQLite
                    PulseiraBDHelper db = PulseiraBDHelper.getInstance(this);
                    ArrayList<Pulseira> offlineList = db.getAllPulseiras();

                    if (!offlineList.isEmpty()) {
                        pulseiras.clear();
                        pulseiras.addAll(offlineList);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Sem internet. A mostrar dados guardados.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Sem internet e sem dados locais.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}