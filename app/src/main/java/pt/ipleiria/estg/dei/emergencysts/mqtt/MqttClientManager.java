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
import pt.ipleiria.estg.dei.emergencysts.activities.comum.MostrarPulseirasActivity;
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
        // Extrai o IP (ex: 172.22.21.215) do URL da API
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
            String clientId = "Android_User_" + userId;

            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();

            // Usa o utilizador que criaste no servidor Ubuntu
            options.setUserName("emergencysts");
            // Usa a password que definiste no comando mosquitto_passwd
            options.setPassword("i%POZsi02Kmc".toCharArray());

            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
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

                    Intent intent = new Intent("MQTT_MESSAGE");
                    intent.putExtra("topic", topic);
                    intent.putExtra("payload", payload);
                    context.sendBroadcast(intent);

                    showSystemNotification(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            if (!client.isConnected()) {
                new Thread(() -> {
                    try {
                        // Liga-se usando as opções com username e password
                        client.connect(options);
                    } catch (MqttException e) {
                        Log.e(TAG, "Erro ao conectar: " + e.getMessage());
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
        String CHANNEL_ID = "emergency_channel_id_v3";

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

            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MostrarPulseirasActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stethoscope)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
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