package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Paciente;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PerfilPacienteActivity extends AppCompatActivity {

    private TextView tvNome, tvEmail, tvDataNasc, tvTelefone, tvSns, tvNif, tvMorada;
    private ImageView btnBack, btnEditar, btnSettings;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_paciente);

        // Inicializar Componentes
        tvNome = findViewById(R.id.tvNomeCompleto);
        tvEmail = findViewById(R.id.tvEmail);
        tvDataNasc = findViewById(R.id.tvDataNasc);
        tvTelefone = findViewById(R.id.tvTelefone);
        tvSns = findViewById(R.id.tvSns);
        tvNif = findViewById(R.id.tvNif);
        tvMorada = findViewById(R.id.tvMorada);

        btnBack = findViewById(R.id.btnBack);
        btnEditar = findViewById(R.id.btnEditar);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);

        // Ações
        btnBack.setOnClickListener(v -> finish());

        btnEditar.setOnClickListener(v -> {
            // Só deixa editar se já tivermos o ID correto (diferente de -1)
            int idPac = SharedPrefManager.getInstance(this).getPaciente().getId();
            if (idPac != -1) {
                startActivity(new Intent(this, EditarPerfilPacienteActivity.class));
            } else {
                Toast.makeText(this, "A carregar dados... Tente novamente.", Toast.LENGTH_SHORT).show();
                carregarPerfilCorreto(); // Tenta buscar outra vez
            }
        });

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, ConfigActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Terminar Sessão")
                    .setMessage("Tem a certeza?")
                    .setPositiveButton("Sim", (d, w) -> SharedPrefManager.getInstance(this).logout())
                    .setNegativeButton("Não", null)
                    .show();
        });

        // Carregar dados ao abrir
        carregarPerfilCorreto();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarPerfilCorreto();
    }

    private void carregarPerfilCorreto() {
        int userIdLogado = SharedPrefManager.getInstance(this).getEnfermeiroBase().getId();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url = baseUrl + "api/paciente?auth_key=" + token;

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean encontrado = false;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject pJson = response.getJSONObject(i);
                            int uId = pJson.optInt("user_id", -1);

                            if (uId == userIdLogado) {
                                encontrado = true;

                                // --- MUDANÇA AQUI: Código muito mais limpo usando o Parser ---
                                // O Parser já trata o ID, Nome, Género, Data, etc.
                                Paciente pacienteObj = pt.ipleiria.estg.dei.emergencysts.utils.PacienteJsonParser.parserJsonPaciente(pJson);

                                if (pacienteObj != null) {
                                    // Garantir que o username vem do login original se não houver no JSON
                                    if (pacienteObj.getUsername().equals("---")) {
                                        // Se precisares de ajustar algo manualmente após o parser:
                                        // ex: pacienteObj.setEmail(pJson.optString("email"));
                                    }

                                    SharedPrefManager.getInstance(this).savePaciente(pacienteObj);
                                    atualizarUI(pacienteObj);
                                }
                                break;
                            }
                        }

                        if (!encontrado) {
                            Toast.makeText(this, "Perfil não encontrado.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erro ao carregar: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );
        req.setShouldCache(false);
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void atualizarUI(Paciente p) {
        tvNome.setText(p.getNome());
        tvEmail.setText(p.getEmail());
        tvDataNasc.setText(p.getDataNascimento());
        tvTelefone.setText(p.getTelefone());
        tvSns.setText(p.getSns());
        tvNif.setText(p.getNif());
        tvMorada.setText(p.getMorada());
    }
}