package pt.ipleiria.estg.dei.emergencysts.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class AtribuirPulseiraActivity extends AppCompatActivity {

    // Campos da Interface
    private EditText etNome, etDataNasc, etSNS, etTelefone;
    private EditText etMotivo, etQueixa, etDescricao, etInicio, etDor, etAlergias, etMedicacao;
    private Spinner spinnerPrioridade;
    private Button btnAtribuir;
    private String pulseiraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atribuir_pulseira);

        //  Inicializar as Views
        // Dados Paciente
        etNome = findViewById(R.id.etNome);
        etDataNasc = findViewById(R.id.etDataNasc);
        etSNS = findViewById(R.id.etSNS);
        etTelefone = findViewById(R.id.etTelefone);

        // Dados Triagem
        etMotivo = findViewById(R.id.etMotivo);
        etQueixa = findViewById(R.id.etQueixa);
        etDescricao = findViewById(R.id.etDescricao);
        etInicio = findViewById(R.id.etInicio);
        etDor = findViewById(R.id.etDor);
        etAlergias = findViewById(R.id.etAlergias);
        etMedicacao = findViewById(R.id.etMedicacao);

        spinnerPrioridade = findViewById(R.id.spinnerPrioridade);
        btnAtribuir = findViewById(R.id.btnAtribuir);
        ImageView btnVoltar = findViewById(R.id.btnVoltar);

        //  Configurar Spinner
        String[] cores = {"Selecione a Prioridade...", "Vermelho", "Laranja", "Amarelo", "Verde", "Azul"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cores);
        spinnerPrioridade.setAdapter(adapter);

        // Receber ID
        pulseiraId = getIntent().getStringExtra("pulseira_id");
        if (pulseiraId == null) {
            finish();
            return;
        }

        carregarDadosTriagem();

        // Listeners
        btnVoltar.setOnClickListener(v -> finish());

        btnAtribuir.setOnClickListener(v -> {
            String prioridadeSelecionada = spinnerPrioridade.getSelectedItem().toString();
            if (prioridadeSelecionada.equals("Selecione a Prioridade...")) {
                Toast.makeText(this, "Selecione uma cor.", Toast.LENGTH_SHORT).show();
            } else {
                guardarAtribuicao(prioridadeSelecionada);
            }
        });
    }

    private void carregarDadosTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String accessToken = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "api/pulseira/" + pulseiraId + "?expand=triagem,userprofile&access-token=" + accessToken;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        //  DESEMBRULHAR
                        JSONObject dadosPulseira = response;
                        if (response.has("data")) {
                            dadosPulseira = response.getJSONObject("data");
                        }

                        //  DADOS DO PACIENTE (Preencher os campos verdes)
                        JSONObject userProfile = dadosPulseira.optJSONObject("userprofile");

                        if (userProfile != null) {
                            etNome.setText(userProfile.optString("nome", "Sem Nome"));
                            etDataNasc.setText(userProfile.optString("datanascimento", "--"));
                            etSNS.setText(userProfile.optString("sns", "--"));
                            etTelefone.setText(userProfile.optString("telefone", "--"));
                        } else {
                            etNome.setText("Não encontrado");
                        }

                        // DADOS DA TRIAGEM
                        JSONObject triagem = dadosPulseira.optJSONObject("triagem");

                        if (triagem != null) {
                            etMotivo.setText(triagem.optString("motivoconsulta", "-"));
                            etQueixa.setText(triagem.optString("queixaprincipal", "-"));
                            etDescricao.setText(triagem.optString("descricaosintomas", "-"));
                            etInicio.setText(triagem.optString("iniciosintomas", "-"));
                            etDor.setText(triagem.optString("intensidadedor", "-"));
                            etAlergias.setText(triagem.optString("alergias", "Não"));
                            etMedicacao.setText(triagem.optString("medicacao", "Nenhuma"));
                        } else {
                            etDescricao.setText("Sem dados de triagem.");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Erro API", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void guardarAtribuicao(String cor) {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String accessToken = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "api/pulseira/" + pulseiraId + "?access-token=" + accessToken;

        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    Toast.makeText(this, "Atribuída com sucesso!", Toast.LENGTH_LONG).show();
                    finish();
                },
                error -> Toast.makeText(this, "Erro ao guardar", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("prioridade", cor);
                params.put("status", "Em espera");
                return params;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}