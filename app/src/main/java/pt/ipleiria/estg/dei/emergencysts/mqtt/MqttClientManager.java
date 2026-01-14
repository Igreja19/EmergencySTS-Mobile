package pt.ipleiria.estg.dei.emergencysts.mqtt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.EnfermeiroActivity;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class MqttClientManager {

    private static final String TAG = "MqttClientManager";
    private static final String CHANNEL_ID = "emergency_channel_id_v3";
    private static MqttClientManager instance;
    private MqttClient client;
    private Context context;

    private MqttClientManager(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    public static synchronized MqttClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttClientManager(context);
        }
        return instance;
    }

    // --- A TUA FUNÇÃO PRESERVADA ---
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
    // -------------------------------

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

            // ID único para evitar conflitos no broker
            String clientId = MqttClient.generateClientId();
            if (SharedPrefManager.getInstance(context).getEnfermeiroBase() != null) {
                clientId = "Android_Enf_" + SharedPrefManager.getInstance(context).getEnfermeiroBase().getId();
            }

            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName("emergencysts");
            options.setPassword("i%POZsi02Kmc".toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(false);
            options.setConnectionTimeout(10);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "Conectado ao MQTT!");
                    subscribeToTopics(); // Chama a lógica de subscrição
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Conexão perdida.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Mensagem MQTT: " + topic + " -> " + payload);

                    // Broadcast para UI
                    Intent broadcastIntent = new Intent("MQTT_MESSAGE");
                    broadcastIntent.putExtra("topic", topic);
                    broadcastIntent.putExtra("payload", payload);
                    context.sendBroadcast(broadcastIntent);

                    try {
                        // 1. Ler JSON
                        JSONObject jsonObject = new JSONObject(payload);

                        String titulo = jsonObject.optString("titulo", "Notificação AMSI");
                        String mensagem = jsonObject.optString("mensagem", payload);

                        Intent intent;

                        // 2. Decidir o Destino
                        // CASO 1: Atualizada/Concluída -> Histórico
                        if (mensagem.toLowerCase().contains("atualizada") || titulo.toLowerCase().contains("concluida")) {
                            intent = new Intent(context, HistoricoActivity.class);
                        }
                        // CASO 2: Nova/Criada -> Lista Pulseiras
                        else if (titulo.toLowerCase().contains("nova") || mensagem.toLowerCase().contains("criada")) {
                            intent = new Intent(context, MostrarPulseirasActivity.class);
                        }
                        // CASO 3: Padrão -> Menu
                        else {
                            intent = new Intent(context, EnfermeiroActivity.class);
                        }

                        // 3. Mostrar Notificação
                        showNotification(titulo, mensagem, intent);

                    } catch (JSONException e) {
                        Log.e(TAG, "Erro JSON: " + e.getMessage());
                        // Fallback se não for JSON
                        showNotification("Alerta", payload, new Intent(context, EnfermeiroActivity.class));
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            if (!client.isConnected()) {
                new Thread(() -> {
                    try {
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

    private void subscribeToTopics() {
        if (client == null || !client.isConnected()) return;

        SharedPrefManager spm = SharedPrefManager.getInstance(context);

        // Usamos a tua função 'subscribe' aqui para manter o código limpo
        if (spm.getPacienteBase() != null && spm.getPacienteBase().getId() != -1) {
            int pid = spm.getPacienteBase().getId();
            subscribe("pulseira/criada/" + pid);
            subscribe("pulseira/atualizada/" + pid);
            Log.d(TAG, "Subscrito como PACIENTE");
        }

        if (spm.getEnfermeiroBase() != null && spm.getEnfermeiroBase().getId() != -1) {
            // Tópico principal do Backend
            subscribe("mosquitto/triagem");
            // Tópico legado (por segurança)
            subscribe("notificacao/enfermeiro");
            Log.d(TAG, "Subscrito como ENFERMEIRO");
        }
    }

    private void showNotification(String title, String messageBody, Intent intent) {
        // Configura o clique na notificação
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)

                .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.green_700))
                .setContentTitle(title)
                .setContentText(messageBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Falta permissão POST_NOTIFICATIONS");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificações Emergência";
            String description = "Alertas de triagem e pulseiras";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void disconnect() {
        new Thread(() -> {
            try {
                if (client != null && client.isConnected()) {
                    client.disconnect();
                    Log.d(TAG, "Desconectado.");
                }
            } catch (MqttException e) {
                Log.e(TAG, "Erro ao desconectar", e);
            }
        }).start();
    }
}