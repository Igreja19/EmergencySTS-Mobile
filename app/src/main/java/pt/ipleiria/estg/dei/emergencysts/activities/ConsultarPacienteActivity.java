package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class ConsultarPacienteActivity extends AppCompatActivity {

    private EditText edtNif;
    private LinearLayout emptyState;
    private CardView resultCard;
    private TextView tvNome, tvNif, tvSns;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultar_paciente);

        edtNif = findViewById(R.id.edtNif);
        emptyState = findViewById(R.id.emptyState);
        resultCard = findViewById(R.id.resultCard);

        tvNome = findViewById(R.id.tvNome);
        tvNif = findViewById(R.id.tvNif);
        tvSns = findViewById(R.id.tvSns);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Quando escrever 9 dígitos
        edtNif.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String nif = s.toString();
                if (nif.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    resultCard.setVisibility(View.GONE);
                    return;
                }
                if (nif.length() == 9) searchPaciente(nif);
            }
        });
    }

    private void searchPaciente(String nif) {

        emptyState.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        // CORREÇÃO IMPORTANTE
        String url = baseUrl + "api/paciente?nif=" + nif + "&auth_key=" + token;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                this::handleResponse,
                error -> showNotFound()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void handleResponse(JSONArray array) {

        System.out.println("🔥 JSON RECEBIDO: " + array.toString());

        if (array.length() == 0) { showNotFound(); return; }

        try {
            JSONObject data = array.getJSONObject(0);

            // Tenta ler de userprofile OU do root
            JSONObject u = data.optJSONObject("userprofile");
            if (u == null) u = data;

            final String nome     = u.optString("nome", "Desconhecido");
            final String nif      = u.optString("nif", "---");
            final String sns      = u.optString("sns", "---");
            final String telefone = u.optString("telefone", "---");
            final String morada   = u.optString("morada", "---");
            final String genero   = u.optString("genero", "---");
            final String dataNasc = u.optString("datanascimento", "---");

            // Atualizar UI
            tvNome.setText(nome);
            tvNif.setText("NIF: " + nif);
            tvSns.setText("SNS: " + sns);

            resultCard.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);

            resultCard.setOnClickListener(v -> {
                Intent intent = new Intent(ConsultarPacienteActivity.this, DetalhesPacienteActivity.class);
                intent.putExtra("nome", nome);
                intent.putExtra("nif", nif);
                intent.putExtra("sns", sns);
                intent.putExtra("telefone", telefone);
                intent.putExtra("morada", morada);
                intent.putExtra("genero", genero);
                intent.putExtra("datanascimento", dataNasc);
                startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
            showNotFound();
        }
    }

    private void showNotFound() {
        Toast.makeText(this, "Paciente não foi encontrado", Toast.LENGTH_SHORT).show();
        resultCard.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
}
