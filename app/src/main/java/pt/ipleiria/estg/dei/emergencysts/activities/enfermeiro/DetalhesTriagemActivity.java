package pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class DetalhesTriagemActivity extends AppCompatActivity {

    private TextView tvNomeValor, tvDataNascimento, tvSNS, tvTelefoneValor, tvDataTriagem;
    private TextView tvMotivo, tvQueixa, tvDescricao, tvInicio, tvDor, tvAlergias, tvMedicacao;
    private TextView tvPrioridade;
    private View dotPrioridade;

    // NOVOS BOTÕES
    private LinearLayout layoutBotoes;
    private Button btnArquivar, btnEliminar;

    private int triagemId;
    private MqttClientManager mqtt;
    private int pulseiraId = -1; // Para guardar o ID da pulseira para arquivar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_triagem);

        triagemId = getIntent().getIntExtra("ID_TRIAGEM", -1);
        if (triagemId == -1) {
            Toast.makeText(this, "Erro: ID inválido.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        getTriagem();

        // MQTT
        mqtt = MqttClientManager.getInstance(this);
        if (mqtt != null) {
            mqtt.connect(this);
            mqtt.subscribe("triagem/atualizada/" + triagemId);
            mqtt.subscribe("pulseira/atualizada/#");
        }
    }

    private void initViews() {
        // Inicializar Texto
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

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // --- CONFIGURAÇÃO DOS 2 BOTÕES ---
        layoutBotoes = findViewById(R.id.layoutBotoesAcao);
        btnArquivar = findViewById(R.id.btnArquivar);
        btnEliminar = findViewById(R.id.btnEliminar);

        String role = SharedPrefManager.getInstance(this).getKeyRole();
        if (role == null) role = "";

        // Se for PACIENTE -> Esconde tudo
        if (role.equalsIgnoreCase("paciente") || role.equalsIgnoreCase("utente")) {
            if (layoutBotoes != null) layoutBotoes.setVisibility(View.GONE);
        } else {
            // Se for ENFERMEIRO -> Mostra e ativa cliques
            if (layoutBotoes != null) layoutBotoes.setVisibility(View.VISIBLE);

            if (btnArquivar != null) {
                btnArquivar.setOnClickListener(v -> confirmarArquivar());
            }
            if (btnEliminar != null) {
                btnEliminar.setOnClickListener(v -> confirmarEliminar());
            }
        }
    }

    private void getTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url = baseUrl + "api/triagem/" + triagemId + "?expand=userprofile,pulseira&auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                this::bindData,
                error -> Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void bindData(JSONObject json) {
        try {
            // Dados Paciente
            JSONObject up = json.optJSONObject("userprofile");
            if (up != null) {
                if(tvNomeValor!=null) tvNomeValor.setText(up.optString("nome", "-"));
                if(tvSNS!=null) tvSNS.setText(up.optString("sns", "-"));
                if(tvDataNascimento!=null) tvDataNascimento.setText(formatData(up.optString("datanascimento", "-")));
                if(tvTelefoneValor!=null) tvTelefoneValor.setText(up.optString("telefone", "-"));
            }

            // Dados Triagem
            if(tvDataTriagem!=null) tvDataTriagem.setText(formatData(json.optString("datatriagem", "-")));
            if(tvMotivo!=null) tvMotivo.setText(json.optString("motivoconsulta", "-"));
            if(tvQueixa!=null) tvQueixa.setText(json.optString("queixaprincipal", "-"));
            if(tvDescricao!=null) tvDescricao.setText(json.optString("descricaosintomas", "-"));
            if(tvInicio!=null) tvInicio.setText(formatData(json.optString("iniciosintomas", "-")));
            if(tvDor!=null) tvDor.setText(String.valueOf(json.optInt("intensidadedor", 0)));
            if(tvAlergias!=null) tvAlergias.setText(json.optString("alergias", "-"));
            if(tvMedicacao!=null) tvMedicacao.setText(json.optString("medicacao", "-"));

            // Pulseira & Cores
            JSONObject pulseira = json.optJSONObject("pulseira");
            if (pulseira != null) {
                pulseiraId = pulseira.optInt("id", -1); // Guardar ID para arquivar
                String prioridade = pulseira.optString("prioridade", "Pendente");
                if(tvPrioridade!=null) tvPrioridade.setText(prioridade);

                int res = R.drawable.circle_gray;
                switch (prioridade.toLowerCase()) {
                    case "vermelho": res = R.drawable.circle_red; break;
                    case "laranja": res = R.drawable.circle_orange; break;
                    case "amarelo": case "amarela": res = R.drawable.circle_yellow; break;
                    case "verde": res = R.drawable.circle_green; break;
                    case "azul": res = R.drawable.circle_blue; break;
                }
                if(dotPrioridade!=null) dotPrioridade.setBackgroundResource(res);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private String formatData(String data) {
        if (data == null || data.equals("null") || data.isEmpty()) return "-";
        try {
            String hora = "";
            if (data.contains("T")) {
                String[] parts = data.split("T");
                data = parts[0];
                if(parts.length > 1 && parts[1].length() >= 5) hora = " " + parts[1].substring(0,5);
            } else if (data.contains(" ")) {
                String[] parts = data.split(" ");
                data = parts[0];
                if(parts.length > 1) hora = " " + parts[1].substring(0,5);
            }
            if (data.contains("-")) {
                String[] p = data.split("-");
                if (p.length == 3) return p[2] + "/" + p[1] + "/" + p[0] + hora;
            }
        } catch (Exception e) {}
        return data;
    }

    // --- AÇÕES DOS BOTÕES ---

    private void confirmarArquivar() {
        if (pulseiraId == -1) {
            Toast.makeText(this, "Não é possível arquivar (sem pulseira).", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Arquivar Triagem")
                .setMessage("Deseja marcar como FINALIZADO? (Isto permite ao paciente fazer nova triagem)")
                .setPositiveButton("Sim, Finalizar", (d, w) -> acaoAPI(true)) // True = Arquivar
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("PERIGO: Eliminar")
                .setMessage("Apagar permanentemente da base de dados?")
                .setPositiveButton("Sim, Apagar", (d, w) -> acaoAPI(false)) // False = Eliminar
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- AQUI ESTÁ A CORREÇÃO PRINCIPAL ---
    private void acaoAPI(boolean isArquivar) {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url;
        final String methodParam; // Define se é PUT ou DELETE

        if (isArquivar) {
            // ARQUIVAR (FINALIZAR):
            // 1. Chama a API da Pulseira
            // 2. Adiciona o sinal "?arquivar=1" para o PHP saber que deve escrever "Finalizado"
            url = baseUrl + "api/pulseira/" + pulseiraId + "?auth_key=" + token + "&arquivar=1";
            methodParam = "PUT"; // PUT = Atualizar
        } else {
            // ELIMINAR (APAGAR MESMO):
            // 1. Chama a API da Triagem
            url = baseUrl + "api/triagem/" + triagemId + "?auth_key=" + token;
            methodParam = "DELETE"; // DELETE = Apagar
        }

        StringRequest req = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (isArquivar) {
                        Toast.makeText(DetalhesTriagemActivity.this, "Arquivado (Finalizado) com sucesso!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DetalhesTriagemActivity.this, "Eliminado permanentemente!", Toast.LENGTH_SHORT).show();
                    }
                    finish(); // Fecha e volta à lista
                },
                error -> {
                    String erro = "Erro de conexão";
                    if (error.networkResponse != null) {
                        erro += " (" + error.networkResponse.statusCode + ")";
                    }
                    Toast.makeText(DetalhesTriagemActivity.this, erro, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // Envia _method para o PHP saber o que fazer (PUT ou DELETE)
                params.put("_method", methodParam);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // MQTT Receivers
    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic != null && (topic.contains("triagem/atualizada/" + triagemId) || topic.startsWith("pulseira/atualizada"))) {
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
        try { unregisterReceiver(mqttReceiver); } catch (Exception e) {}
    }
}