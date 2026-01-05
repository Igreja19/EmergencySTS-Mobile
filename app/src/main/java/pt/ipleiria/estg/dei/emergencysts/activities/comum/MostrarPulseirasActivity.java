package pt.ipleiria.estg.dei.emergencysts.activities.comum;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color; // Importante para as cores
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.AtribuirPulseiraActivity;
import pt.ipleiria.estg.dei.emergencysts.adapters.PulseiraAdapter;
import pt.ipleiria.estg.dei.emergencysts.listeners.PulseiraListener;
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraBDHelper;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraJsonParser;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class MostrarPulseirasActivity extends AppCompatActivity implements PulseiraListener {

    // Comuns
    private ProgressBar progressBar;
    private LinearLayout layoutSemPulseira;
    private MqttClientManager mqtt;
    private boolean isPaciente;
    private TextView tvTitulo, tvSubtitulo;

    // Parte do Enfermeiro (Lista)
    private ListView listViewPulseiras;
    private PulseiraAdapter adapter;
    private ArrayList<Pulseira> listaPulseiras = new ArrayList<>();

    // Parte do Paciente (Cartão)
    private CardView cardPulseira;
    private TextView tvEstadoBadge, tvCodigoPulseira, tvDescricao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrar_pulseiras);

        // Verificar quem é o user
        isPaciente = getIntent().getBooleanExtra("IS_PACIENTE", false);

        // Views Comuns
        progressBar = findViewById(R.id.progressBar);
        layoutSemPulseira = findViewById(R.id.layoutSemPulseira);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        if (isPaciente) {
            // Inicializar Views do Paciente
            cardPulseira = findViewById(R.id.cardPulseira);
            tvEstadoBadge = findViewById(R.id.tvEstadoBadge);
            tvCodigoPulseira = findViewById(R.id.tvCodigoPulseira);
            tvDescricao = findViewById(R.id.tvDescricao);

            tvTitulo.setText("A minha Pulseira");
            tvSubtitulo.setText("Acompanhe o seu estado");

            ListView lv = findViewById(R.id.listViewPulseiras);
            if(lv != null) lv.setVisibility(View.GONE);

        } else {
            // Inicializar Views do Enfermeiro
            listViewPulseiras = findViewById(R.id.listViewPulseiras);
            adapter = new PulseiraAdapter(this, listaPulseiras, this);
            listViewPulseiras.setAdapter(adapter);

            tvTitulo.setText("Pulseiras");
            tvSubtitulo.setText("Pulseiras em espera para triagem");

            CardView cv = findViewById(R.id.cardPulseira);
            if(cv != null) cv.setVisibility(View.GONE);
        }

        // MQTT
        mqtt = MqttClientManager.getInstance(this);
        mqtt.connect(this);
        mqtt.subscribe("pulseira/atualizada/#");
        mqtt.subscribe("pulseira/criada/#");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        getPulseirasAPI();

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
        } catch (Exception e) { }
    }

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic != null && (topic.startsWith("pulseira/atualizada/") || topic.startsWith("pulseira/criada/"))) {
                getPulseirasAPI();
            }
        }
    };

    private void getPulseirasAPI() {
        progressBar.setVisibility(View.VISIBLE);
        layoutSemPulseira.setVisibility(View.GONE);

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String authKey = SharedPrefManager.getInstance(this).getKeyAccessToken();

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("api/pulseira?");

        if (isPaciente) {
            // Remover filtro de status para o Paciente ver pulseiras triadas
            // Filtramos apenas por user (o backend já faz isso pelo token) e ordenamos por ID decrescente
            urlBuilder.append("sort=-id&");
        } else {
            // ENFERMEIRO: Vê apenas pulseiras não triadas
            urlBuilder.append("status=Em%20espera&prioridade=Pendente&");
        }
        urlBuilder.append("expand=userprofile&auth_key=").append(authKey);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, urlBuilder.toString(), null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray data = response.has("data") ? response.getJSONArray("data") : response.optJSONArray("items");

                        ArrayList<Pulseira> novas = new ArrayList<>();
                        if (data != null) {
                            novas = PulseiraJsonParser.parserJsonPulseiras(data);

                            // Guardar BD
                            PulseiraBDHelper db = PulseiraBDHelper.getInstance(this);
                            db.removeAllPulseiras();
                            for (Pulseira p : novas) db.adicionarPulseira(p);
                        }

                        atualizarInterface(novas);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    PulseiraBDHelper db = PulseiraBDHelper.getInstance(this);
                    atualizarInterface(db.getAllPulseiras());
                    Toast.makeText(this, "Modo Offline", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void atualizarInterface(ArrayList<Pulseira> pulseiras) {
        if (pulseiras == null || pulseiras.isEmpty()) {
            layoutSemPulseira.setVisibility(View.VISIBLE);
            if (isPaciente) cardPulseira.setVisibility(View.GONE);
            else listViewPulseiras.setVisibility(View.GONE);
            return;
        }

        if (isPaciente) {
            // --- UI PACIENTE ---
            Pulseira p = pulseiras.get(0);
            cardPulseira.setVisibility(View.VISIBLE);
            layoutSemPulseira.setVisibility(View.GONE);

            //  MOSTRAR O NOME NO TÍTULO
            if (p.getNomePaciente() != null && !p.getNomePaciente().isEmpty()) {
                tvTitulo.setText("Olá, " + p.getNomePaciente());
            } else {
                tvTitulo.setText("A minha Pulseira");
            }

            //  Preencher o resto (Código e Estado)
            tvCodigoPulseira.setText("#" + p.getCodigo());

            String prioridade = p.getPrioridade();
            String status = p.getStatus();

            if (prioridade != null && !prioridade.equalsIgnoreCase("Pendente")) {
                // Já foi triado - Mostrar a Cor
                tvEstadoBadge.setText(prioridade);
                tvDescricao.setText("Triagem concluída. Aguarde chamada.");

                // Mudar cor do badge conforme a prioridade
                switch (prioridade.toLowerCase()) {
                    case "vermelho":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_red); // Usa os teus drawables
                        break;
                    case "laranja":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_orange);
                        break;
                    case "amarelo":
                        tvEstadoBadge.setTextColor(Color.BLACK); // Texto preto para fundo amarelo
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_yellow);
                        break;
                    case "verde":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.card_green_rounded);
                        break;
                    case "azul":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_blue);
                        break;
                    default:
                        tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_pendente);
                }
            } else {
                // Ainda não foi triado (Pendente)
                tvEstadoBadge.setText("Pendente");
                tvEstadoBadge.setTextColor(Color.parseColor("#D84315"));
                tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_pendente);
                tvDescricao.setText("Aguarde na sala de espera pela triagem.");
            }

        } else {
            // --- UI ENFERMEIRO ---
            listViewPulseiras.setVisibility(View.VISIBLE);
            listaPulseiras.clear();
            listaPulseiras.addAll(pulseiras);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPulseiraClick(Pulseira pulseira) {
        if (!isPaciente) {
            Intent intent = new Intent(this, AtribuirPulseiraActivity.class);
            intent.putExtra("pulseira_id", pulseira.getId());
            startActivity(intent);
        }
    }
}