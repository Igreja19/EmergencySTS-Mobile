package pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class DetalhesTriagemActivity extends AppCompatActivity {

    private TextView tvNomeValor, tvDataNascimento, tvSNS, tvTelefoneValor, tvDataTriagem;
    private TextView tvMotivo, tvQueixa, tvDescricao, tvInicio, tvDor, tvAlergias, tvMedicacao;
    private TextView tvPrioridade;
    private View dotPrioridade;
    private Button btnApagar;

    private int triagemId;
    private MqttClientManager mqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_triagem);

        // Receber o ID da triagem selecionada
        triagemId = getIntent().getIntExtra("ID_TRIAGEM", -1);
        if (triagemId == -1) {
            Toast.makeText(this, "Erro: Triagem inválida ou ID não recebido.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        getTriagem(); // Carregar dados da API

        // Configurar MQTT
        mqtt = MqttClientManager.getInstance(this);
        // Verificar conexão antes de subscrever
        if (mqtt != null) {
            mqtt.connect(this);
            mqtt.subscribe("triagem/atualizada/" + triagemId);
            mqtt.subscribe("pulseira/atualizada/#");
        }
    }

    private void initViews() {
        // Inicializar as Views de Texto
        tvNomeValor = findViewById(R.id.tvNomeValor);
        tvDataNascimento = findViewById(R.id.tvDataNascimento);
        tvSNS = findViewById(R.id.tvSNS);
        tvTelefoneValor = findViewById(R.id.tvTelefoneValor);
        tvDataTriagem = findViewById(R.id.tvDataTriagem);

        tvMotivo = findViewById(R.id.tvMotivo);
        tvQueixa = findViewById(R.id.tvQueixa);
        tvDescricao = findViewById(R.id.tvDescricao);
        tvInicio = findViewById(R.id.tvInicio);
        tvDor = findViewById(R.id.tvDor);
        tvAlergias = findViewById(R.id.tvAlergias);
        tvMedicacao = findViewById(R.id.tvMedicacao);

        tvPrioridade = findViewById(R.id.tvPrioridade);
        dotPrioridade = findViewById(R.id.dotPrioridade);

        //  Botão Voltar
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // LÓGICA DE SEGURANÇA DO BOTÃO APAGAR
        btnApagar = findViewById(R.id.btnApagar);

        if (btnApagar != null) {
            // Obter a role do utilizador atual usando o método do teu print
            String role = SharedPrefManager.getInstance(this).getKeyRole();

            // Verificação de segurança (caso a role venha nula)
            if (role == null) role = "";

            // Se for paciente, ESCONDE o botão (GONE remove o espaço também)
            // Adicionei "utente" também, só para garantir
            if (role.equalsIgnoreCase("paciente") || role.equalsIgnoreCase("utente")) {
                btnApagar.setVisibility(View.GONE);
            }
            else {
                // Se for enfermeiro/médico, MOSTRA e ativa o clique
                btnApagar.setVisibility(View.VISIBLE);
                btnApagar.setOnClickListener(v -> confirmarEliminacao());
            }
        }
    }

    private void getTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // Mantenho a expansão, caso precises dos dados no futuro, mas o código abaixo só usa o que o XML mostra
        String url = baseUrl + "api/triagem/" + triagemId +
                "?expand=userprofile,pulseira&auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("TRIAGEM_DATA", response.toString());
                    bindData(response);
                },
                error -> {
                    Log.e("VOLLEY_ERROR", error.toString());
                    Toast.makeText(this, "Erro ao carregar triagem.", Toast.LENGTH_SHORT).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void bindData(JSONObject json) {
        try {
            // Dados do Paciente ---
            JSONObject up = json.optJSONObject("userprofile");
            if (up != null) {
                if(tvNomeValor != null) tvNomeValor.setText(up.optString("nome", "-"));
                if(tvSNS != null) tvSNS.setText(up.optString("sns", "-"));
                if(tvDataNascimento != null) tvDataNascimento.setText(formatData(up.optString("datanascimento", "-")));
                if(tvTelefoneValor != null) tvTelefoneValor.setText(up.optString("telefone", "-"));
            }

            //  Dados da Triagem ---
            String dataRegisto = json.optString("datatriagem", "-").replace("T", " ");
            if(tvDataTriagem != null) tvDataTriagem.setText(dataRegisto);
            if(tvMotivo != null) tvMotivo.setText(json.optString("motivoconsulta", "-"));
            if(tvQueixa != null) tvQueixa.setText(json.optString("queixaprincipal", "-"));
            if(tvDescricao != null) tvDescricao.setText(json.optString("descricaosintomas", "-"));

            // Tratamento da data de início de sintomas
            String inicioSintomas = json.optString("iniciosintomas", "-").replace("T", " ");
            if(tvInicio != null) tvInicio.setText(inicioSintomas);

            if(tvAlergias != null) tvAlergias.setText(json.optString("alergias", "-"));
            if(tvMedicacao != null) tvMedicacao.setText(json.optString("medicacao", "-"));
            if(tvDor != null) tvDor.setText(String.valueOf(json.optInt("intensidadedor", 0)));

            // Prioridade (Pulseira) ---
            JSONObject pulseira = json.optJSONObject("pulseira");
            if (pulseira != null) {
                String prioridade = pulseira.optString("prioridade", "Pendente");
                if(tvPrioridade != null) tvPrioridade.setText(prioridade);

                int res;
                switch (prioridade.toLowerCase()) {
                    case "vermelho": res = R.drawable.circle_red; break;
                    case "laranja": res = R.drawable.circle_orange; break;
                    case "amarelo":
                    case "amarela": res = R.drawable.circle_yellow; break;
                    case "verde": res = R.drawable.circle_green; break;
                    case "azul": res = R.drawable.circle_blue; break;
                    default: res = R.drawable.circle_gray;
                }
                if (dotPrioridade != null) {
                    dotPrioridade.setBackgroundResource(res);
                }
            }

            // NOTA: Removi a lógica do Enfermeiro e Data/Hora da triagem
            // porque não tens onde mostrar isso no XML que enviaste.

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DetalhesTriagem", "Erro no bindData: " + e.getMessage());
            Toast.makeText(this, "Erro ao processar dados visualmente.", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatData(String data) {
        if (data == null || data.equals("null")) return "-";
        try {
            // Se vier com hora (T), limpamos primeiro
            if (data.contains("T")) {
                data = data.split("T")[0];
            }

            // Converte YYYY-MM-DD para DD/MM/YYYY
            if (data.contains("-")) {
                String[] p = data.split("-");
                if (p.length == 3) {
                    return p[2] + "/" + p[1] + "/" + p[0];
                }
            }
        } catch (Exception e) {
            Log.e("DetalhesTriagem", "Erro formatação data: " + data);
        }
        return data;
    }

    //  MQTT Receiver
    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic != null) {
                if(topic.contains("triagem/atualizada/" + triagemId) || topic.startsWith("pulseira/atualizada")) {
                    getTriagem();
                }
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
        try {
            unregisterReceiver(mqttReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver não estava registado
        }
    }

    // Funcionalidade de Apagar
    private void confirmarEliminacao() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Apagar Triagem");
        builder.setMessage("Tem a certeza que deseja apagar esta triagem? Esta ação é irreversível.");

        builder.setPositiveButton("Sim", (dialog, which) -> deleteTriagem());
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void deleteTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url = baseUrl + "api/triagem/" + triagemId + "?auth_key=" + token;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "Triagem apagada com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    String erro = "Erro ao apagar.";
                    if (error.networkResponse != null) {
                        erro += " Código: " + error.networkResponse.statusCode;
                    }
                    Toast.makeText(this, erro, Toast.LENGTH_SHORT).show();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}