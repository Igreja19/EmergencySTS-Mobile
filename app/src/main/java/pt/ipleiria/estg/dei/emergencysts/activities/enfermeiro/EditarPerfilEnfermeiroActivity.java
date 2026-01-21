package pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Enfermeiro;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class EditarPerfilEnfermeiroActivity extends AppCompatActivity {

    private EditText etNome, etEmail, etTelefone, etMorada, etNif, etSns;
    private ProgressBar progressBar;
    private Enfermeiro original;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil_enfermeiro);

        // Inicialização de UI
        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etMorada = findViewById(R.id.etMorada);
        etNif = findViewById(R.id.etNif);
        etSns = findViewById(R.id.etSns);
        progressBar = findViewById(R.id.progressBar);

        ImageView btnCancel = findViewById(R.id.btnCancel);
        Button btnSaveBottom = findViewById(R.id.btnSaveBottom);

        // Carregar dados guardados
        original = SharedPrefManager.getInstance(this).getEnfermeiro();
        carregarDadosAtuais();

        // Listeners
        btnCancel.setOnClickListener(v -> finish());
        btnSaveBottom.setOnClickListener(v -> guardarAlteracoes());
    }

    private void carregarDadosAtuais() {
        if (original != null) {
            etNome.setText(original.getNome());
            etEmail.setText(original.getEmail());
            etTelefone.setText(original.getTelefone());
            etMorada.setText(original.getMorada());
            etNif.setText(original.getNif());
            etSns.setText(original.getSns());
        }
    }

    private void guardarAlteracoes() {
        // Verifica se o objeto original existe antes de tudo
        if (original == null) {
            original = SharedPrefManager.getInstance(this).getEnfermeiro();
            if (original == null || original.getUserId() <= 0) {
                Toast.makeText(this, "Erro: Dados do enfermeiro não encontrados.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);

        // URL: O VolleySingleton já coloca a auth_key no fim, não mexa aqui.
        String url = VolleySingleton.getInstance(this).getAPIUrl(VolleySingleton.ENDPOINT_ENFERMEIRO + "/" + original.getUserId());
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    atualizarSharedPrefsLocalmente();
                    Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_LONG).show();
                    finish();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    if (error.networkResponse != null) {
                        // Verifique o Logcat para ler o erro real do Yii2 (ex: validação falhou)
                        String errorData = new String(error.networkResponse.data);
                        Log.e("API_ERRO", "Status: " + error.networkResponse.statusCode + " Body: " + errorData);
                    }
                    Toast.makeText(this, "Erro ao guardar alterações", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                params.put("_method", "PUT");

                params.put("Enfermeiro[nome]", etNome.getText().toString().trim());
                params.put("Enfermeiro[email]", etEmail.getText().toString().trim());
                params.put("Enfermeiro[telefone]", etTelefone.getText().toString().trim());
                params.put("Enfermeiro[morada]", etMorada.getText().toString().trim());
                params.put("Enfermeiro[nif]", etNif.getText().toString().trim());
                params.put("Enfermeiro[sns]", etSns.getText().toString().trim());

                if (original.getDataNascimento() != null) {
                    params.put("Enfermeiro[datanascimento]", original.getDataNascimento());
                }

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(getApplicationContext()).getKeyAccessToken();
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                // Garante que o servidor entenda que enviamos um formulário
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void atualizarSharedPrefsLocalmente() {
        if (original != null) {
            original.setNome(etNome.getText().toString().trim());
            original.setEmail(etEmail.getText().toString().trim());
            original.setTelefone(etTelefone.getText().toString().trim());
            original.setMorada(etMorada.getText().toString().trim());
            original.setNif(etNif.getText().toString().trim());
            original.setSns(etSns.getText().toString().trim());

            SharedPrefManager.getInstance(this).saveEnfermeiro(original);
        }
    }
}