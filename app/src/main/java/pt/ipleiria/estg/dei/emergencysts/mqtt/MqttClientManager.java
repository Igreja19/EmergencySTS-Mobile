package pt.ipleiria.estg.dei.emergencysts.mqtt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.comum.HistoricoActivity;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class MqttClientManager {

    private static final String TAG = "MqttClientManager";
    private static MqttClientManager instance;
    private MqttClient client;
    private Context context;

    private MqttClientManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized MqttClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttClientManager(context);
        }
        return instance;
    }

    public void connect() {
        if (!SharedPrefManager.getInstance(context).isLoggedIn()) {
            return;
        }

        // 1. Lógica de IP dinâmico (Server ou Local)
        String serverIp = SharedPrefManager.getInstance(context).getServerBase();

        // Remove http, https e barras extra para ficar só o IP ou Hostname
        String cleanIp = serverIp.replace("http://", "").replace("https://", "").replace("/", "");

        // Se tiver porta (ex: :8080), removemos para usar a porta padrão do MQTT (1883)
        if(cleanIp.contains(":")) {
            cleanIp = cleanIp.split(":")[0];
        }

        String brokerUrl = "tcp://" + cleanIp + ":1883";
        Log.d(TAG, "Conectando ao Broker: " + brokerUrl);

        try {
            if (client != null && client.isConnected()) {
                return;
            }

            // 2. Client ID único usando o ID do utilizador
            int userId = SharedPrefManager.getInstance(context).getEnfermeiroBase().getId();
            String clientId = "Android_" + userId + "_" + System.currentTimeMillis();

            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false); // False = recebe mensagens que perdeu enquanto estava offline
            options.setConnectionTimeout(10);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "Conectado ao MQTT!");
                    subscribeUserTopic(); // Subscrever assim que conecta
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Conexão perdida.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Notificação recebida: " + payload);

                    // A. Broadcast para atualizar a app se estiver aberta
                    Intent intent = new Intent("MQTT_MESSAGE");
                    intent.putExtra("topic", topic);
                    intent.putExtra("payload", payload);
                    context.sendBroadcast(intent);

                    // B. Criar a Notificação Visual (Barra de topo)
                    showSystemNotification(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            if (!client.isConnected()) {
                new Thread(() -> {
                    try {
                        client.connect(options);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        if (client != null && client.isConnected()) {
            try {
                client.subscribe(topic, 1); // QoS 1 garante a entrega
                Log.d(TAG, "Subscrito com sucesso: " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "Erro ao subscrever tópico: " + topic, e);
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "Cliente não conectado. Não foi possível subscrever: " + topic);
        }
    }

    private void subscribeUserTopic() {
        if (client != null && client.isConnected()) {
            // Lógica Crítica: Usamos o ID base (User ID) que serve tanto para Pacientes como Enfermeiros
            // O getEnfermeiroBase() retorna os dados do User (Login), incluindo o ID correto para o tópico.
            int userId = SharedPrefManager.getInstance(context).getEnfermeiroBase().getId();

            String topic = "notificacao/nova/" + userId;

            try {
                client.subscribe(topic, 1);
                Log.d(TAG, "Subscrito ao tópico: " + topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void showSystemNotification(String payload) {
        String CHANNEL_ID = "emergency_channel_id";
        String titulo = "Nova Notificação";
        String mensagem = payload;

        try {
            JSONObject json = new JSONObject(payload);
            titulo = json.optString("titulo", "Emergência");
            mensagem = json.optString("mensagem", "Tem uma nova mensagem.");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificações Emergência";
            String description = "Alertas importantes";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, HistoricoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stethoscope)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Permissão em falta.");
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}