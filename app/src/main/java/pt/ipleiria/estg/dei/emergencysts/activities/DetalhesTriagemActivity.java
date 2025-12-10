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

    private TextView tvNomeValor, tvDataNascimento, tvSNS, tvTelefoneValor;
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

        initViews();
        getTriagem();
    }

    private void initViews() {
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
        dotPrioridade = findViewById(R.id.dotPrioridade);

        tvEnfermeiro = findViewById(R.id.tvEnfermeiro);
        tvData = findViewById(R.id.tvData);
        tvHora = findViewById(R.id.tvHora);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void getTriagem() {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "/api/triagem/" + triagemId + "?auth_key=" + token;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> bindData(response),
                error -> Toast.makeText(this, "Erro ao carregar triagem.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void bindData(JSONObject json) {
        try {
            // ----- USERPROFILE -----
            JSONObject up = json.optJSONObject("userprofile");
            if (up != null) {
                tvNomeValor.setText(up.optString("nome", ""));

                tvSNS.setText(up.optString("sns", "-"));
                tvDataNascimento.setText(up.optString("datanascimento", "-"));
                tvTelefoneValor.setText(up.optString("telefone", "-"));
            }

            // ----- TRIAGEM -----
            tvMotivo.setText(json.optString("motivoconsulta", "-"));
            tvQueixa.setText(json.optString("queixaprincipal", "-"));
            tvDescricao.setText(json.optString("descricaosintomas", "-"));
            tvInicio.setText(json.optString("iniciosintomas", "-"));
            tvAlergias.setText(json.optString("alergias", "-"));
            tvMedicacao.setText(json.optString("medicacao", "-"));

            // Intensidade dor → tens o campo
            tvDor.setText(String.valueOf(json.optInt("intensidadedor", 0)));

            // ----- DATA + HORA -----
            String dataHora = json.optString("datatriagem", "");
            if (dataHora.contains(" ")) {
                String[] partes = dataHora.split(" ");
                tvData.setText(formatData(partes[0]));
                tvHora.setText(partes[1].substring(0, 5));
            }

            // ----- PULSEIRA -----
            JSONObject pulseira = json.optJSONObject("pulseira");
            if (pulseira != null) {
                String prioridade = pulseira.optString("prioridade", "Pendente");
                tvPrioridade.setText(prioridade);

                switch (prioridade.toLowerCase()) {
                    case "vermelha": dotPrioridade.setBackgroundResource(R.drawable.circle_red); break;
                    case "laranja":  dotPrioridade.setBackgroundResource(R.drawable.circle_orange); break;
                    case "amarela":  dotPrioridade.setBackgroundResource(R.drawable.circle_yellow); break;
                    case "verde":    dotPrioridade.setBackgroundResource(R.drawable.circle_green); break;
                    case "azul":     dotPrioridade.setBackgroundResource(R.drawable.circle_blue); break;
                    default:         dotPrioridade.setBackgroundResource(R.drawable.circle_gray);
                }
            }

            // ----- ENFERMEIRO RESPONSÁVEL -----
            tvEnfermeiro.setText("Enf. " + SharedPrefManager.getInstance(this).getUser().getUsername());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatData(String data) {
        String[] p = data.split("-");
        return p[2] + "/" + p[1] + "/" + p[0];
    }
}
