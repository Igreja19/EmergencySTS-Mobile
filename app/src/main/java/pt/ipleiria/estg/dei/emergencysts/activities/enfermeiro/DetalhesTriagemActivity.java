package pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class DetalhesTriagemActivity extends AppCompatActivity {

    private TextView tvNomeValor, tvDataNascimento, tvSNS, tvTelefoneValor;
    private TextView tvMotivo, tvQueixa, tvDescricao, tvInicio, tvDor, tvAlergias, tvMedicacao;
    private TextView tvPrioridade, tvEnfermeiro, tvData, tvHora;
    private android.view.View dotPrioridade;

    private int triagemId;
    private MqttClientManager mqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_triagem);

        triagemId = getIntent().getIntExtra("triagem_id", -1);
        if (triagemId == -1) {
            Toast.makeText(this, "Erro: Triagem inválida.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        getTriagem();

        mqtt = MqttClientManager.getInstance(this);
        mqtt.connect(this);
        mqtt.subscribe("triagem/atualizada/" + triagemId);
        mqtt.subscribe("pulseira/atualizada/#");
    }

    private void initViews() {
        tvNomeValor = findViewById(R.id.tvNomeValor);
        tvDataNascimento = findViewById(R.id.tvDataNascimento);
        tvSNS = findViewById(R.id.tvSNS);
        tvTelefoneValor = findViewById(R.id.tvTelefoneValor);

        tvMotivo = findViewById(R.id.tvMotivo);
        tvQueixa = findViewById(R.id.tvQueixa);
        tvDescricao = findViewById(R.id.tvDescricao);
        tvInicio = findViewById(R.id.tvInicio);
        tvDor = findViewById(R.id.tvDor);
        tvAlergias = findViewById(R.id.tvAlergias);
        tvMedicacao = findViewById(R.id.tvMedicacao);

        tvPrioridade = findViewById(R.id.tvPrioridade);
        dotPrioridade = findViewById(R.id.dotPrioridade);

        tvEnfermeiro = findViewById(R.id.tvEnfermeiro);
        tvData = findViewById(R.id.tvData);
        tvHora = findViewById(R.id.tvHora);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void getTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String url = baseUrl + "api/triagem/" + triagemId + "?auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                this::bindData,
                error -> Toast.makeText(this, "Erro ao carregar triagem.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void bindData(JSONObject json) {
        try {
            JSONObject up = json.optJSONObject("userprofile");
            if (up != null) {
                tvNomeValor.setText(up.optString("nome", "-"));
                tvSNS.setText(up.optString("sns", "-"));
                tvDataNascimento.setText(up.optString("datanascimento", "-"));
                tvTelefoneValor.setText(up.optString("telefone", "-"));
            }

            tvMotivo.setText(json.optString("motivoconsulta", "-"));
            tvQueixa.setText(json.optString("queixaprincipal", "-"));
            tvDescricao.setText(json.optString("descricaosintomas", "-"));
            tvInicio.setText(json.optString("iniciosintomas", "-"));
            tvAlergias.setText(json.optString("alergias", "-"));
            tvMedicacao.setText(json.optString("medicacao", "-"));
            tvDor.setText(String.valueOf(json.optInt("intensidadedor", 0)));

            String dataHora = json.optString("datatriagem", "");
            if (dataHora.contains(" ")) {
                String[] partes = dataHora.split(" ");
                tvData.setText(formatData(partes[0]));
                tvHora.setText(partes[1].substring(0, 5));
            }

            JSONObject pulseira = json.optJSONObject("pulseira");
            if (pulseira != null) {
                String prioridade = pulseira.optString("prioridade", "Pendente");
                tvPrioridade.setText(prioridade);

                int res;
                switch (prioridade.toLowerCase()) {
                    case "vermelha": res = R.drawable.circle_red; break;
                    case "laranja": res = R.drawable.circle_orange; break;
                    case "amarela": res = R.drawable.circle_yellow; break;
                    case "verde": res = R.drawable.circle_green; break;
                    case "azul": res = R.drawable.circle_blue; break;
                    default: res = R.drawable.circle_gray;
                }
                dotPrioridade.setBackgroundResource(res);
            }

        } catch (Exception ignored) {}
    }

    private String formatData(String data) {
        String[] p = data.split("-");
        return p[2] + "/" + p[1] + "/" + p[0];
    }

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic == null) return;

            if (topic.contains("triagem/atualizada/" + triagemId)) {
                getTriagem();
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
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
}
