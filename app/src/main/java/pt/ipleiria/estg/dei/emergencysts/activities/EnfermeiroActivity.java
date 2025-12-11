package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
            Intent intent = new Intent(this, MostrarPulseirasActivity.class);
            startActivity(intent);
        });
        cardConsultarPaciente.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConsultarPacienteActivity.class);
            startActivity(intent);
        });
        cardHistoricoTriagem.setOnClickListener(v ->{
            Intent intent = new Intent(this, HistoricoActivity.class);
            startActivity(intent);
        });
        cardPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilEnfermeiroActivity.class);
            startActivity(intent);
        });
    }
}
