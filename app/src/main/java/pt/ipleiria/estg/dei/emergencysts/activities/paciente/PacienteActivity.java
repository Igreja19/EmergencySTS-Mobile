package pt.ipleiria.estg.dei.emergencysts.activities.paciente;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.comum.HistoricoActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.comum.MostrarPulseirasActivity;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PacienteActivity extends AppCompatActivity {

    private TextView tvTitulo, tvSubtitulo;
    private CardView cardPulseira, cardHistorico, cardPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Área do Paciente");
        }

        tvTitulo = findViewById(R.id.tvTitulo);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        cardPulseira = findViewById(R.id.cardPulseira);
        cardHistorico = findViewById(R.id.cardHistorico);
        cardPerfil = findViewById(R.id.cardPerfil);

        // ✅ CORREÇÃO: ir buscar o PACIENTE
        String username = SharedPrefManager.getInstance(this)
                .getPacienteBase()
                .getUsername();

        tvTitulo.setText("Emergency STS");
        tvSubtitulo.setText("Área do Paciente");

        Toast.makeText(this, "Bem-vindo, " + username + "!", Toast.LENGTH_LONG).show();

        cardPulseira.setOnClickListener(v -> {
            Intent intent = new Intent(this, MostrarPulseirasActivity.class);
            intent.putExtra("IS_PACIENTE", true);
            startActivity(intent);
        });

        cardHistorico.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoricoActivity.class);
            intent.putExtra("IS_PACIENTE", true);
            startActivity(intent);
        });

        cardPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, PerfilPacienteActivity.class))
        );
    }

    // MQTT deve ser garantido aqui
    @Override
    protected void onResume() {
        super.onResume();
        MqttClientManager.getInstance(this).connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Isso garante que notificações cheguem mesmo que a Activity seja destruída
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
