package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.adapters.TriagemAdapter;
import pt.ipleiria.estg.dei.emergencysts.modelo.Triagem;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;
import pt.ipleiria.estg.dei.emergencysts.utils.TriagemJsonParser;

public class HistoricoActivity extends AppCompatActivity {

    private ListView listViewTriagens;
    private TextView tvTotalTriagens;

    private TriagemAdapter adapter;
    private ArrayList<Triagem> triagens = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        listViewTriagens = findViewById(R.id.listViewTriagens);
        tvTotalTriagens = findViewById(R.id.tvTotalTriagens);

        adapter = new TriagemAdapter(this, triagens);
        listViewTriagens.setAdapter(adapter);

        listViewTriagens.setOnItemClickListener((parent, view, position, id) -> {
            Triagem t = triagens.get(position);

            Intent intent = new Intent(HistoricoActivity.this, DetalhesTriagemActivity.class);
            intent.putExtra("triagem_id", t.id);
            startActivity(intent);
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getTriagens();
    }

    private void getTriagens() {

        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        // 🔥 NOVO ENDPOINT: apenas triagens com consulta encerrada
        String url = baseUrl + "/api/triagem/historico?auth_key=" + token;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                this::onSuccess,
                error -> Toast.makeText(this, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void onSuccess(JSONArray response) {
        try {
            triagens.clear();

            ArrayList<Triagem> novas = TriagemJsonParser.parserJsonTriagens(response);

            triagens.addAll(novas);
            adapter.notifyDataSetChanged();

            tvTotalTriagens.setText("Total de triagens: " + triagens.size());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
        }
    }
}
