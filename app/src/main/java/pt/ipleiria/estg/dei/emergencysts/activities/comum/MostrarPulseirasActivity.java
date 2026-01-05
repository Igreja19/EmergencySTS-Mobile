package pt.ipleiria.estg.dei.emergencysts.activities.comum;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
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
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.AtribuirPulseiraActivity;
import pt.ipleiria.estg.dei.emergencysts.adapters.PulseiraAdapter;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraBDHelper;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraJsonParser;
import pt.ipleiria.estg.dei.emergencysts.listeners.PulseiraListener;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class MostrarPulseirasActivity extends AppCompatActivity implements PulseiraListener {

    private ListView listViewPulseiras;
    private ProgressBar progressBar;
    private PulseiraAdapter adapter;
    private ArrayList<Pulseira> pulseiras = new ArrayList<>();

    private MqttClientManager mqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrar_pulseiras);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        listViewPulseiras = findViewById(R.id.listViewPulseiras);
        progressBar = findViewById(R.id.progressBar);

        adapter = new PulseiraAdapter(this, pulseiras, this);
        listViewPulseiras.setAdapter(adapter);

        mqtt = MqttClientManager.getInstance(this);
        mqtt.connect(this);
        mqtt.subscribe("pulseira/atualizada/#");
        mqtt.subscribe("pulseira/criada/#");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        getPulseiras();

        IntentFilter filter = new IntentFilter("MQTT_MESSAGE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mqttReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mqttReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mqttReceiver);
        } catch (Exception e) {
            // Receiver pode não estar registado
        }
    }

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic == null) return;

            if (topic.startsWith("pulseira/atualizada/") ||
                    topic.startsWith("pulseira/criada/")) {
                getPulseiras();
            }
        }
    };

    private void getPulseiras() {
        progressBar.setVisibility(View.VISIBLE);

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String authKey = SharedPrefManager.getInstance(this).getKeyAccessToken();

        // Verificar se é paciente
        boolean isPaciente = getIntent().getBooleanExtra("IS_PACIENTE", false);

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("api/pulseira?");

        // --- LÓGICA CORRIGIDA FINAL ---
        if (isPaciente) {
            // PACIENTE: Vê "Em espera" (pendentes/ativas), mas IGNORA a prioridade.
            // Assim vê as suas pulseiras mesmo que já tenham cor (Amarela/Verde), mas não vê as Concluídas.
            urlBuilder.append("status=Em%20espera&");
        } else {
            // ENFERMEIRO: Vê apenas as que estão "Em espera" E "Pendente" (sem triagem feita)
            urlBuilder.append("status=Em%20espera&prioridade=Pendente&");
        }

        urlBuilder.append("expand=userprofile&auth_key=").append(authKey);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                urlBuilder.toString(),
                null,
                response -> {
                    try {
                        JSONArray data = response.has("data") ? response.getJSONArray("data") : response.optJSONArray("items");
                        if (data != null) {
                            ArrayList<Pulseira> novas = PulseiraJsonParser.parserJsonPulseiras(data);
                            pulseiras.clear();
                            pulseiras.addAll(novas);
                            adapter.notifyDataSetChanged();

                            // Guardar na BD para acesso Offline
                            PulseiraBDHelper db = PulseiraBDHelper.getInstance(this);
                            db.removeAllPulseiras();
                            for (Pulseira p : novas) db.adicionarPulseira(p);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    progressBar.setVisibility(View.GONE);

                    // Carregar da BD se falhar a net
                    PulseiraBDHelper db = PulseiraBDHelper.getInstance(this);
                    ArrayList<Pulseira> offline = db.getAllPulseiras();
                    pulseiras.clear();
                    pulseiras.addAll(offline);
                    adapter.notifyDataSetChanged();

                    if (!offline.isEmpty()) {
                        Toast.makeText(this, "Sem internet. A mostrar dados guardados.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Erro ao carregar pulseiras.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    @Override
    public void onPulseiraClick(Pulseira pulseira) {
        boolean isPaciente = getIntent().getBooleanExtra("IS_PACIENTE", false);

        if (isPaciente) {
            // Paciente vê apenas info
            Toast.makeText(this, "Estado: " + pulseira.getStatus(), Toast.LENGTH_SHORT).show();
        } else {
            // Enfermeiro vai atribuir
            Intent intent = new Intent(this, AtribuirPulseiraActivity.class);
            intent.putExtra("pulseira_id", pulseira.getId());
            startActivity(intent);
        }
    }
}