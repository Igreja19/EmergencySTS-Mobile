package pt.ipleiria.estg.dei.emergencysts.mqtt;

import android.content.Context;
import android.content.Intent;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttClientManager {

    private static MqttClientManager instance;
    private MqttClient client;

    private MqttClientManager(Context context) {}

    public static synchronized MqttClientManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttClientManager(context);
        }
        return instance;
    }

    public void connect(Context context) {
        try {
            if (client == null) {
                client = new MqttClient("tcp://10.0.2.2:1883", MqttClient.generateClientId(), null);
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {}

                @Override
                public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Intent intent = new Intent("MQTT_MESSAGE");
                    intent.putExtra("topic", topic);
                    intent.putExtra("payload", message.toString());
                    context.sendBroadcast(intent);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            if (!client.isConnected()) {
                client.connect(options);
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic, 0);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
