package pt.ipleiria.estg.dei.emergencysts.activities.comum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException; // Importante para ler o JSON da mensagem
import org.json.JSONObject;

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
            if (lv != null) lv.setVisibility(View.GONE);

        } else {
            // Inicializar Views do Enfermeiro
            listViewPulseiras = findViewById(R.id.listViewPulseiras);
            adapter = new PulseiraAdapter(this, listaPulseiras, this);
            listViewPulseiras.setAdapter(adapter);

            tvTitulo.setText("Pulseiras");
            tvSubtitulo.setText("Pulseiras em espera para triagem");

            CardView cv = findViewById(R.id.cardPulseira);
            if (cv != null) cv.setVisibility(View.GONE);
        }

        // MQTT - Inicializar e Subscrever
        mqtt = MqttClientManager.getInstance(this);
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
        } catch (Exception e) {
        }
    }

    // --- RECEIVER MQTT COM NOTIFICAÇÕES ---
    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String topic = intent.getStringExtra("topic");
            // CORREÇÃO: Tem de ser "payload" porque é assim que envias no MqttClientManager
            String messageJson = intent.getStringExtra("payload");

            if (topic != null) {
                // 1. Atualiza sempre a lista
                getPulseirasAPI();

                // 2. Lógica de Notificações
                String textoNotificacao = "Toque para ver detalhes";

                // Tenta extrair a mensagem bonita do JSON se existir
                if (messageJson != null) {
                    try {
                        JSONObject json = new JSONObject(messageJson);
                        if (json.has("mensagem")) {
                            textoNotificacao = json.getString("mensagem");
                        } else if (json.has("corpo")) {
                            textoNotificacao = json.getString("corpo");
                        }
                    } catch (JSONException e) {
                        // Se não for JSON, usa a string direta ou texto padrão
                        if(!messageJson.isEmpty()) textoNotificacao = messageJson;
                    }
                }

                if (isPaciente) {
                    // Se for Paciente: Avise se a pulseira foi atualizada (triagem feita)
                    if (topic.startsWith("pulseira/atualizada/")) {
                        criarNotificacao("Estado Atualizado", "A sua pulseira foi atualizada.");
                    }
                } else {
                    // Se for Enfermeiro: Avisa se uma NOVA pulseira chegou
                    if (topic.startsWith("pulseira/criada/")) {
                        criarNotificacao("Nova Pulseira!", textoNotificacao);
                    }
                }
            }
        }
    };

    // --- MÉTODO PARA CRIAR A NOTIFICAÇÃO ---
    private void criarNotificacao(String titulo, String mensagem) {
        // Verifica permissões para Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Se não tiver permissão, não faz nada (no futuro podes pedir aqui)
                return;
            }
        }

        String CHANNEL_ID = "canal_pulseiras_emergencia";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 1. Criar Canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificações de Pulseiras",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisos de novas entradas e triagens");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // 2. Intent para abrir a App ao clicar
        Intent intent = new Intent(this, MostrarPulseirasActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Construir
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Troca pelo teu R.drawable.logo se tiveres
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Som e Vibração
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 4. Mostrar
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void getPulseirasAPI() {
        progressBar.setVisibility(View.VISIBLE);
        layoutSemPulseira.setVisibility(View.GONE);

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String authKey = SharedPrefManager.getInstance(this).getKeyAccessToken();

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("api/pulseira?");

        if (isPaciente) {
            // Ordena por ID decrescente para apanhar a última pulseira criada
            urlBuilder.append("sort=-id&");
        } else {
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

                            // BD Offline
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

        req.setShouldCache(false);
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
            Pulseira p = pulseiras.get(0); // Apanha a pulseira mais recente
            String status = p.getStatus();

            // Verifica se está nulo, vazio, ou com estado finalizado
            boolean estaFinalizada = status == null ||
                    status.trim().isEmpty() ||
                    status.equalsIgnoreCase("null") ||
                    status.equalsIgnoreCase("Finalizado") ||
                    status.equalsIgnoreCase("Concluída") ||
                    status.equalsIgnoreCase("Concluida") ||
                    status.equalsIgnoreCase("Atendido");

            if (estaFinalizada) {
                // Se o estado for VAZIO ou finalizado, esconde a pulseira
                layoutSemPulseira.setVisibility(View.VISIBLE);
                cardPulseira.setVisibility(View.GONE);
                return;
            }

            // Se chegou aqui, é porque a pulseira ESTÁ ATIVA
            cardPulseira.setVisibility(View.VISIBLE);
            layoutSemPulseira.setVisibility(View.GONE);

            // Nome
            if (p.getNomePaciente() != null && !p.getNomePaciente().isEmpty()) {
                tvTitulo.setText("Olá, " + p.getNomePaciente());
            } else {
                tvTitulo.setText("A minha Pulseira");
            }

            // Código
            tvCodigoPulseira.setText("#" + p.getCodigo());

            String prioridade = p.getPrioridade();

            if (prioridade != null && !prioridade.equalsIgnoreCase("Pendente")) {
                // Triado
                tvEstadoBadge.setText(prioridade);
                tvDescricao.setText("Triagem concluída. Aguarde chamada.");

                switch (prioridade.toLowerCase()) {
                    case "vermelho":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_red);
                        break;
                    case "laranja":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_orange);
                        break;
                    case "amarelo":
                        tvEstadoBadge.setTextColor(Color.BLACK);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_yellow);
                        break;
                    case "verde":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_green);
                        break;
                    case "azul":
                        tvEstadoBadge.setTextColor(Color.WHITE);
                        tvEstadoBadge.setBackgroundResource(R.drawable.circle_blue);
                        break;
                    default:
                        tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_pendente);
                }
            } else {
                // Pendente
                tvEstadoBadge.setText("Pendente");
                tvEstadoBadge.setTextColor(Color.parseColor("#D84315"));
                tvEstadoBadge.setBackgroundResource(R.drawable.bg_chip_pendente);
                tvDescricao.setText("Aguarde na sala de espera pela triagem.");
            }

        } else {
            //UI ENFERMEIRO
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Isto resolve o aviso "no usages" e previne erros de memória
        if (mqtt != null) {
            mqtt.disconnect();
        }
    }
}