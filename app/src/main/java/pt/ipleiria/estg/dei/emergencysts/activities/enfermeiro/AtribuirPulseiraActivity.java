package pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.PulseiraJsonParser;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class AtribuirPulseiraActivity extends AppCompatActivity {

    private EditText etNome, etDataNasc, etSNS, etTelefone;
    private EditText etMotivo, etQueixa, etDescricao, etInicio, etDor, etAlergias, etMedicacao;
    private Spinner spinnerPrioridade;
    private Button btnAtribuir;

    private String pulseiraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atribuir_pulseira);

        inicializarCampos();

        String[] cores = {"Selecione a Prioridade...", "Vermelho", "Laranja", "Amarelo", "Verde", "Azul"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cores);
        spinnerPrioridade.setAdapter(adapter);

        pulseiraId = getIntent().getStringExtra("pulseira_id");

        if (pulseiraId == null) {
            Toast.makeText(this, "Erro: ID em falta", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        carregarDadosTriagem();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        btnAtribuir.setOnClickListener(v -> {
            String prioridade = spinnerPrioridade.getSelectedItem().toString();

            if (prioridade.equals("Selecione a Prioridade...")) {
                Toast.makeText(this, "Selecione uma cor.", Toast.LENGTH_SHORT).show();
            } else {
                guardarAtribuicao(prioridade);
            }
        });
    }

    private void inicializarCampos() {
        etNome = findViewById(R.id.etNome);
        etDataNasc = findViewById(R.id.etDataNasc);
        etSNS = findViewById(R.id.etSNS);
        etTelefone = findViewById(R.id.etTelefone);

        etMotivo = findViewById(R.id.etMotivo);
        etQueixa = findViewById(R.id.etQueixa);
        etDescricao = findViewById(R.id.etDescricao);
        etInicio = findViewById(R.id.etInicio);
        etDor = findViewById(R.id.etDor);
        etAlergias = findViewById(R.id.etAlergias);
        etMedicacao = findViewById(R.id.etMedicacao);

        spinnerPrioridade = findViewById(R.id.spinnerPrioridade);
        btnAtribuir = findViewById(R.id.btnAtribuir);
    }

    /**
     * GET: Carregar os dados completos da Pulseira + Triagem + Userprofile
     */
    private void carregarDadosTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String authKey = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "api/pulseira/" + pulseiraId +
                "?expand=triagem,userprofile&auth_key=" + authKey;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Pulseira p = PulseiraJsonParser.parserJsonPulseira(response);

                    if (p != null) {
                        etNome.setText(p.getNomePaciente());
                        etDataNasc.setText(p.getDataNascimento());
                        etSNS.setText(p.getSns());
                        etTelefone.setText(p.getTelefone());

                        etMotivo.setText(p.getMotivo());
                        etQueixa.setText(p.getQueixa());
                        etDescricao.setText(p.getDescricao());
                        etInicio.setText(p.getInicioSintomas());
                        etDor.setText(String.valueOf(p.getDor()));
                        etAlergias.setText(p.getAlergias());
                        etMedicacao.setText(p.getMedicacao());
                    } else {
                        Toast.makeText(this, "Erro ao ler dados da pulseira.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erro ao ligar à API.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    /**
     * PUT: Atribuir prioridade da Pulseira
     */
    private void guardarAtribuicao(String cor) {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String authKey = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "api/pulseira/" + pulseiraId + "?auth_key=" + authKey;

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    Toast.makeText(this, "Pulseira atribuída com sucesso!", Toast.LENGTH_LONG).show();
                    finish();
                },
                error -> Toast.makeText(this, "Erro ao guardar atribuição.", Toast.LENGTH_SHORT).show()
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
