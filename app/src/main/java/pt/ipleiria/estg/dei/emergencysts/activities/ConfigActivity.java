package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class ConfigActivity extends AppCompatActivity {

    private EditText editServerUrl;
    private Button btnSave, btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        SharedPrefManager pref = SharedPrefManager.getInstance(this);

        editServerUrl = findViewById(R.id.editServerUrl);
        btnSave       = findViewById(R.id.btnSave);
        btnReset      = findViewById(R.id.btnReset);
        ImageView btnBack = findViewById(R.id.btnBack);

        // Preencher campo (se houver valor guardado)
        editServerUrl.setText(pref.getServerUrl());

        // Botão voltar
        btnBack.setOnClickListener(v -> finish());

        // Limpar campo
        btnReset.setOnClickListener(v -> editServerUrl.setText(""));

        // Guardar configuração
        btnSave.setOnClickListener(v -> {
            String url = editServerUrl.getText().toString().trim();

            if (!url.startsWith("http")) {
                editServerUrl.setError("O URL deve começar por http ou https");
                return;
            }

            pref.setServerUrl(url);

            Toast.makeText(this, "Configuração guardada!", Toast.LENGTH_SHORT).show();

            // Vai para login
            Intent i = new Intent(ConfigActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
}
