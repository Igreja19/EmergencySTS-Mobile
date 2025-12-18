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
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PacienteActivity extends AppCompatActivity {

    private TextView tvTitulo, tvSubtitulo;
    private CardView cardPulseira, cardHistorico, cardPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente);

        // 🔹 Ativar o botão "←" na ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Área do Paciente");
        }

        // 🔹 Inicializar elementos de interface
        tvTitulo = findViewById(R.id.tvTitulo);
        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        cardPulseira = findViewById(R.id.cardPulseira);
        cardHistorico = findViewById(R.id.cardHistorico);
        cardPerfil = findViewById(R.id.cardPerfil);

        // 🔹 Obter o nome do utilizador guardado
        String username = SharedPrefManager.getInstance(this).getEnfermeiro().getUsername();

        tvTitulo.setText("Emergency STS");
        tvSubtitulo.setText("Área do Paciente");

        Toast.makeText(this, "Bem-vindo, " + username + "!", Toast.LENGTH_LONG).show();

        // 🔹 Eventos de clique
        cardPulseira.setOnClickListener(v -> startActivity(new Intent(this, MostrarPulseirasActivity.class)));
        cardHistorico.setOnClickListener(v -> startActivity(new Intent(this, HistoricoActivity.class)));
        cardPerfil.setOnClickListener(v -> startActivity(new Intent(this, PerfilPacienteActivity.class)));
    }

    // 🔹 Trata o clique do botão "Voltar" da ActionBar
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
