package pt.ipleiria.estg.dei.emergencysts.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class DetalhesTriagemActivity extends AppCompatActivity {

    private TextView tvNome, tvNomeValor, tvDataNascimento, tvSNS, tvTelefoneValor;
    private TextView tvMotivo, tvQueixa, tvDescricao, tvInicio, tvDor, tvAlergias, tvMedicacao;
    private TextView tvPrioridade, tvEnfermeiro, tvData, tvHora;
    private View dotPrioridade;

    private int triagemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_triagem);

        triagemId = getIntent().getIntExtra("triagem_id", -1);

        if (triagemId == -1) {
            Toast.makeText(this, "Erro: Triagem inválida.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initUI();
        loadTriagem();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void initUI() {
        tvNome = findViewById(R.id.tvNome);
        tvNomeValor = findViewById(R.id.tvNomeValor);
        tvDataNascimento = findViewById(R.id.tvDataNascimento);
        tvSNS = findViewById(R.id.tvSNS);
        tvTelefoneValor = findViewById(R.id.tvTelefoneValor);

        tvMotivo = findViewById(R.id.tvMotivo);
        tvQueixa = findViewById(R.id.tvQueixa);
        tvDescricao = findViewById(R.id.tvDescricao);
        tvInicio = findViewById(R.id.tvInicio);
        tvDor = findViewById(R.id.tvDor);
        tvAlergias = findViewById(R.id.tvAlergias);
        tvMedicacao = findViewById(R.id.tvMedicacao);

        tvPrioridade = findViewById(R.id.tvPrioridade);
        tvEnfermeiro = findViewById(R.id.tvEnfermeiro);
        tvData = findViewById(R.id.tvData);
        tvHora = findViewById(R.id.tvHora);

        dotPrioridade = findViewById(R.id.dotPrioridade);

        Button btnApagar = findViewById(R.id.btnApagar);
        btnApagar.setOnClickListener(v -> confirmarApagar());
    }

    private void loadTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String url = baseUrl + "/api/triagem/" + triagemId + "?auth_key=" + token;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                this::parseAndFill,
                error -> Toast.makeText(this, "Erro ao carregar detalhes.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void parseAndFill(JSONObject json) {
        try {
            // USERPROFILE
            JSONObject up = json.optJSONObject("userprofile");
            if (up != null) {
                tvNomeValor.setText(up.optString("nome", "-"));
                tvDataNascimento.setText(up.optString("datanascimento", "-"));
                tvSNS.setText(up.optString("sns", "-"));
                tvTelefoneValor.setText(up.optString("telefone", "-"));
            } else {
                tvNomeValor.setText("-");
                tvDataNascimento.setText("-");
                tvSNS.setText("-");
                tvTelefoneValor.setText("-");
            }

            // INFO CLÍNICA
            tvMotivo.setText(json.optString("motivoconsulta", "-"));
            tvQueixa.setText(json.optString("queixaprincipal", "-"));
            tvDescricao.setText(json.optString("descricaosintomas", "-"));
            tvInicio.setText(json.optString("iniciosintomas", "-"));
            tvDor.setText(String.valueOf(json.optInt("intensidadedor", 0)));
            tvAlergias.setText(json.optString("alergias", "-"));
            tvMedicacao.setText(json.optString("medicacao", "-"));

            // PULSEIRA
            JSONObject pulseira = json.optJSONObject("pulseira");
            if (pulseira != null) {
                String prioridade = pulseira.optString("prioridade", "-");
                tvPrioridade.setText(prioridade);

                switch (prioridade.toLowerCase()) {
                    case "vermelha": dotPrioridade.setBackgroundResource(R.drawable.circle_red); break;
                    case "laranja":  dotPrioridade.setBackgroundResource(R.drawable.circle_orange); break;
                    case "amarela":  dotPrioridade.setBackgroundResource(R.drawable.circle_yellow); break;
                    case "verde":    dotPrioridade.setBackgroundResource(R.drawable.circle_green); break;
                    case "azul":     dotPrioridade.setBackgroundResource(R.drawable.circle_blue); break;
                    default:         dotPrioridade.setBackgroundResource(R.drawable.circle_gray);
                }

                tvEnfermeiro.setText(pulseira.optString("responsavel", "—"));
            } else {
                tvPrioridade.setText("-");
                dotPrioridade.setBackgroundResource(R.drawable.circle_gray);
                tvEnfermeiro.setText("—");
            }

            // DATA E HORA
            String data = json.optString("datatriagem", "");
            if (data.length() >= 16) {
                tvData.setText(data.substring(0, 10));
                tvHora.setText(data.substring(11, 16));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarApagar() {
        new AlertDialog.Builder(this)
                .setTitle("Apagar Triagem")
                .setMessage("Tem a certeza que deseja apagar esta triagem?")
                .setPositiveButton("Sim", (dialog, which) -> apagarTriagem())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void apagarTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String url = baseUrl + "/api/triagem/" + triagemId + "?auth_key=" + token;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    Toast.makeText(this, "Triagem apagada.", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> Toast.makeText(this, "Erro ao apagar triagem.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}
