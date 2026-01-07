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

        String serverIp = SharedPrefManager.getInstance(context).getServerBase();
        String cleanIp = serverIp.replace("http://", "").replace("https://", "").replace("/", "");

        if(cleanIp.contains(":")) {
            cleanIp = cleanIp.split(":")[0];
        }

        String brokerUrl = "tcp://" + cleanIp + ":1883";
        Log.d(TAG, "Conectando ao Broker: " + brokerUrl);

        try {
            if (client != null && client.isConnected()) {
                return;
            }

            int userId = SharedPrefManager.getInstance(context).getEnfermeiroBase().getId();

            // CORREÇÃO: ID fixo para suportar CleanSession(false).
            // Se usares timestamp, o broker acha que é sempre um telemóvel novo.
            String clientId = "Android_User_" + userId;

            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(false); // Recebe mensagens perdidas
            options.setConnectionTimeout(10);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "Conectado ao MQTT!");
                    subscribeUserTopic();
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

                    // B. Criar a Notificação Visual
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
                client.subscribe(topic, 1);
                Log.d(TAG, "Subscrito com sucesso: " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "Erro ao subscrever tópico: " + topic, e);
            }
        }
    }

    private void subscribeUserTopic() {
        if (client != null && client.isConnected()) {
            int userId = SharedPrefManager.getInstance(context).getEnfermeiroBase().getId();
            String topic = "notificacao/nova/" + userId;
            try {
                client.subscribe(topic, 1);
                Log.d(TAG, "Subscrito ao tópico pessoal: " + topic);
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
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, HistoricoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stethoscope) // Garante que este ícone existe
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
                Log.d(TAG, "MQTT Desconectado.");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}