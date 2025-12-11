package pt.ipleiria.estg.dei.emergencysts.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;

public class EnfermeiroActivity extends AppCompatActivity {

    private CardView cardMostrarPulseira, cardConsultarPaciente, cardHistoricoTriagem, cardPerfil;
    private MqttClientManager mqtt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enfermeiro);

        mqtt = MqttClientManager.getInstance(this);
        mqtt.connect(this);

        mqtt.subscribe("triagem/atualizada/#");
        mqtt.subscribe("pulseira/atualizada/#");
        mqtt.subscribe("consulta/atualizada/#");

        cardMostrarPulseira = findViewById(R.id.cardMostrarPulseira);
        cardConsultarPaciente = findViewById(R.id.cardConsultarPaciente);
        cardHistoricoTriagem = findViewById(R.id.cardHistoricoTriagem);
        cardPerfil = findViewById(R.id.cardPerfil);

        cardMostrarPulseira.setOnClickListener(v -> startActivity(new Intent(this, MostrarPulseirasActivity.class)));
        cardConsultarPaciente.setOnClickListener(v -> startActivity(new Intent(this, ConsultarPacienteActivity.class)));
        cardHistoricoTriagem.setOnClickListener(v -> startActivity(new Intent(this, HistoricoActivity.class)));
        cardPerfil.setOnClickListener(v -> startActivity(new Intent(this, PerfilEnfermeiroActivity.class)));
    }

    private final BroadcastReceiver mqttReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String topic = intent.getStringExtra("topic");
            if (topic != null) {
                Toast.makeText(context, "MQTT recebido: " + topic, Toast.LENGTH_SHORT).show();
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
