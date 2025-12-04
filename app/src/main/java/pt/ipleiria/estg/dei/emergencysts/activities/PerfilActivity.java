package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.User;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PerfilActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvNome, tvEmail, tvDataNasc, tvIdade, tvTelefone, tvSns, tvNif, tvMorada;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        // 🔗 Ligação ao XML
        btnBack = findViewById(R.id.btnBack);
        ImageView btnSettings = findViewById(R.id.btnSettings);

        tvNome      = findViewById(R.id.tvNomeCompleto);
        tvEmail     = findViewById(R.id.tvEmail);
        tvDataNasc  = findViewById(R.id.tvDataNasc);
        tvIdade     = findViewById(R.id.tvIdade);
        tvTelefone  = findViewById(R.id.tvTelefone);
        tvSns       = findViewById(R.id.tvSns);
        tvNif       = findViewById(R.id.tvNif);
        tvMorada    = findViewById(R.id.tvMorada);

        btnLogout   = findViewById(R.id.btnLogout);

        // 🔙 Botão voltar
        btnBack.setOnClickListener(v -> finish());

        // ⚙️ Botão configurações → abrir ConfigActivity
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilActivity.this, ConfigActivity.class);
            startActivity(intent);
        });

        // 📌 Preencher dados locais
        carregarDadosLocais();

        // 🔐 Logout
        btnLogout.setOnClickListener(v -> {
            SharedPrefManager.getInstance(this).logout();
        });
    }

    private void carregarDadosLocais() {
        User user = SharedPrefManager.getInstance(this).getUser();

        if (user == null) return;

        tvNome.setText(user.getNomeCompleto() != null ? user.getNomeCompleto() : "---");
        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "---");
        tvDataNasc.setText(user.getDataNascimento() != null ? user.getDataNascimento() : "---");
        tvIdade.setText(user.getIdadeFormatada() != null ? user.getIdadeFormatada() : "-- anos");
        tvTelefone.setText(user.getTelefone() != null ? user.getTelefone() : "---");
        tvSns.setText(user.getSns() != null ? user.getSns() : "---");
        tvNif.setText(user.getNif() != null ? user.getNif() : "---");
        tvMorada.setText(user.getMorada() != null ? user.getMorada() : "---");
    }
}
