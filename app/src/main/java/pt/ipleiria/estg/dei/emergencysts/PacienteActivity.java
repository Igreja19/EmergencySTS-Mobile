package pt.ipleiria.estg.dei.emergencysts;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PacienteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);

        findViewById(R.id.cardPulseira).setOnClickListener(v -> Toast.makeText(this, "Abrir Pulseira", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardHistorico).setOnClickListener(v -> Toast.makeText(this, "Abrir Historico", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardPerfil).setOnClickListener(v -> Toast.makeText(this, "Abrir Perfil", Toast.LENGTH_SHORT).show());
    }
}