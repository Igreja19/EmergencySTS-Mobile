package pt.ipleiria.estg.dei.emergencysts.activities.paciente;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import java.util.HashMap;
import java.util.Map;
import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Paciente;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class EditarPerfilPacienteActivity extends AppCompatActivity {

    private EditText etNome, etEmail, etTelefone, etMorada, etNif, etSns;
    private ProgressBar progressBar;
    private Paciente original;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil_paciente);

        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etMorada = findViewById(R.id.etMorada);
        etNif = findViewById(R.id.etNif);
        etSns = findViewById(R.id.etSns);
        progressBar = findViewById(R.id.progressBar);

        original = SharedPrefManager.getInstance(this).getPaciente();
        carregarDadosAtuais();

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveBottom).setOnClickListener(v -> guardarAlteracoes());
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
        if (original == null) return;

        final String nome = etNome.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();

        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nome e Email são obrigatórios!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String url = VolleySingleton.getInstance(this).getAPIUrl(VolleySingleton.ENDPOINT_PACIENTE + "/" + original.getId());

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    atualizarSharedPrefsLocalmente();
                    Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_LONG).show();
                    finish();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    String body = (error.networkResponse != null) ? new String(error.networkResponse.data) : "Erro de rede";
                    Log.e("API_ERRO", body);
                    Toast.makeText(this, "Erro ao guardar alterações", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("_method", "PUT");
                params.put("Paciente[nome]", nome);
                params.put("Paciente[email]", email);
                params.put("Paciente[telefone]", etTelefone.getText().toString().trim());
                params.put("Paciente[morada]", etMorada.getText().toString().trim());
                params.put("Paciente[nif]", etNif.getText().toString().trim());
                params.put("Paciente[sns]", etSns.getText().toString().trim());

                if (original.getGenero() != null) params.put("Paciente[genero]", original.getGenero());
                if (original.getDataNascimento() != null) params.put("Paciente[datanascimento]", original.getDataNascimento());

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = SharedPrefManager.getInstance(getApplicationContext()).getKeyAccessToken();
                if (token != null) headers.put("Authorization", "Bearer " + token);
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void atualizarSharedPrefsLocalmente() {
        if (original != null) {
            original.setNome(etNome.getText().toString());
            original.setEmail(etEmail.getText().toString());
            original.setTelefone(etTelefone.getText().toString());
            original.setMorada(etMorada.getText().toString());
            original.setNif(etNif.getText().toString());
            original.setSns(etSns.getText().toString());
            SharedPrefManager.getInstance(this).savePaciente(original);
        }
    }
}