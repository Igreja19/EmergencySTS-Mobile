package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import pt.ipleiria.estg.dei.emergencysts.ConsultarPaciente;
import pt.ipleiria.estg.dei.emergencysts.R;

public class EnfermeiroActivity extends AppCompatActivity {

    private CardView cardMostrarPulseira, cardConsultarPaciente, cardHistoricoTriagem, cardPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enfermeiro);

        // 🔹 Inicializa cartões
        cardMostrarPulseira = findViewById(R.id.cardMostrarPulseira);
        cardConsultarPaciente = findViewById(R.id.cardConsultarPaciente);
        cardHistoricoTriagem = findViewById(R.id.cardHistoricoTriagem);
        cardPerfil = findViewById(R.id.cardPerfil);

        // 🔹 Ações
        cardMostrarPulseira.setOnClickListener(v -> {
            Intent intent = new Intent(this, PulseirasActivity.class);
            startActivity(intent);
        });
        cardConsultarPaciente.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConsultarPaciente.class);
            startActivity(intent);
        });
        cardHistoricoTriagem.setOnClickListener(v ->
                Toast.makeText(this, "Abrir histórico de triagens", Toast.LENGTH_SHORT).show());
        cardPerfil.setOnClickListener(v ->
                Toast.makeText(this, "Abrir perfil do enfermeiro", Toast.LENGTH_SHORT).show());
    }
}
