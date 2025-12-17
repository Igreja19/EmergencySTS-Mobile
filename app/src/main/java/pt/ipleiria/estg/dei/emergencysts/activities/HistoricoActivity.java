package pt.ipleiria.estg.dei.emergencysts.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.adapters.TriagemAdapter;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.modelo.Triagem;
import pt.ipleiria.estg.dei.emergencysts.listeners.TriagemListener;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;
import pt.ipleiria.estg.dei.emergencysts.utils.TriagemJsonParser;

public class HistoricoActivity extends AppCompatActivity implements TriagemListener {

    private ListView listViewTriagens;
    private TextView tvTotalTriagens;
    private TriagemAdapter adapter;
    private ArrayList<Triagem> triagens = new ArrayList<>();
    private MqttClientManager mqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        listViewTriagens = findViewById(R.id.listViewTriagens);
        tvTotalTriagens = findViewById(R.id.tvTotalTriagens);

        adapter = new TriagemAdapter(this, triagens, this);
        listViewTriagens.setAdapter(adapter);


        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        mqtt = MqttClientManager.getInstance(this);
        mqtt.connect(this);

        mqtt.subscribe("triagem/atualizada/#");
        mqtt.subscribe("consulta/atualizada/#");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        getTriagens();

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
        unregisterReceiver(mqttReceiver);
    }

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic == null) return;

            if (topic.startsWith("triagem/atualizada/") ||
                    topic.startsWith("consulta/atualizada/")) {

                getTriagens();
                Toast.makeText(ctx, "Histórico atualizado", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void getTriagens() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String url = baseUrl + "/api/triagem/historico?auth_key=" + token;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                this::onSuccess,
                error -> Toast.makeText(this, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void onSuccess(JSONArray response) {
        try {
            triagens.clear();

            ArrayList<Triagem> todas = TriagemJsonParser.parserJsonTriagens(response);
            ArrayList<Triagem> filtradas = new ArrayList<>();

            for (Triagem t : todas) {

                boolean prioridadeValida =
                        t.pulseira != null &&
                                t.pulseira.prioridade != null &&
                                !t.pulseira.prioridade.equalsIgnoreCase("Pendente");

                boolean consultaEncerrada =
                        t.consulta != null &&
                                t.consulta.estado != null &&
                                t.consulta.estado.equalsIgnoreCase("Encerrada");

                // apenas triagens com prioridade + consulta encerrada
                if (prioridadeValida && consultaEncerrada) {
                    filtradas.add(t);
                }
            }

            triagens.addAll(filtradas);
            adapter.notifyDataSetChanged();
            tvTotalTriagens.setText("Total de triagens: " + triagens.size());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTriagemClick(int id) {
        // Esta lógica vem para aqui, vinda da Interface
        Intent intent = new Intent(this, DetalhesTriagemActivity.class);
        intent.putExtra("triagem_id", id);
        startActivity(intent);
    }

}
