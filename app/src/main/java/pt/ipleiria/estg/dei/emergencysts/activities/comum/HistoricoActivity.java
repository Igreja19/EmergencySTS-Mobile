package pt.ipleiria.estg.dei.emergencysts.activities.comum;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.DetalhesTriagemActivity;
import pt.ipleiria.estg.dei.emergencysts.adapters.TriagemAdapter;
import pt.ipleiria.estg.dei.emergencysts.listeners.TriagemListener;
import pt.ipleiria.estg.dei.emergencysts.modelo.Triagem;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;
import pt.ipleiria.estg.dei.emergencysts.utils.TriagemJsonParser;

public class HistoricoActivity extends AppCompatActivity implements TriagemListener {

    private ListView listView;
    private TriagemAdapter adapter;
    private TextView tvTitulo, tvTotalTriagens;
    private ImageView btnBack;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isPaciente;
    private ArrayList<Triagem> listaTriagens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);
        tvTitulo = findViewById(R.id.tvTitulo);
        tvTotalTriagens = findViewById(R.id.tvTotalTriagens);
        btnBack = findViewById(R.id.btnBack);
        listView = findViewById(R.id.listViewTriagens);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);


        listaTriagens = new ArrayList<>();

        // Verificar Role com segurança
        String role = "";
        if (SharedPrefManager.getInstance(this).getEnfermeiroBase() != null) {
            role = SharedPrefManager.getInstance(this).getEnfermeiroBase().getRole();
        }
        isPaciente = role != null && (role.equalsIgnoreCase("paciente") || role.equalsIgnoreCase("utente"));

        // Configurar UI
        tvTitulo.setText(isPaciente ? "O Meu Histórico" : "Histórico Geral");

        btnBack.setOnClickListener(v -> finish());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::carregarHistorico);
        }

        // Configurar lista
        adapter = new TriagemAdapter(this, listaTriagens, !isPaciente, this);
        listView.setAdapter(adapter);

        // Clique para ver detalhes
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < listaTriagens.size()) {
                Triagem t = listaTriagens.get(position);
                onTriagemClick(t.getId());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarHistorico();
    }

    private void carregarHistorico() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url = baseUrl + "api/triagem?auth_key=" + token + "&expand=paciente,pulseira,userprofile";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    try {
                        listaTriagens.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            Triagem t = TriagemJsonParser.parserJsonTriagem(obj);

                            String prioridade = "Pendente";
                            String status = "Desconhecido";

                            if (t.pulseira != null) {
                                if (t.pulseira.prioridade != null) prioridade = t.pulseira.prioridade;
                                if (t.pulseira.getStatus() != null) status = t.pulseira.getStatus();
                            }

                            //  Pendente não aparece no histórico (ainda não foi triado)
                            if (prioridade.equalsIgnoreCase("Pendente")) continue;

                            //  O Enfermeiro não vê o que já acabou
                            if (!isPaciente) {
                                String s = status.trim();
                                if (s.equalsIgnoreCase("Finalizado") ||
                                        s.equalsIgnoreCase("Concluída") ||
                                        s.equalsIgnoreCase("Concluida") ||
                                        s.equalsIgnoreCase("Atendido")) { // <--- Esconde "Atendido"
                                    continue;
                                }
                            }

                            listaTriagens.add(t);
                        }

                        adapter.notifyDataSetChanged();

                        if (tvTotalTriagens != null) {
                            tvTotalTriagens.setText("Total de triagens: " + listaTriagens.size());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show();
                }
        );

        req.setShouldCache(false);
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    //  INTERFACE TriagemListener

    @Override
    public void onTriagemClick(int id) {
        Intent intent = new Intent(this, DetalhesTriagemActivity.class);
        intent.putExtra("ID_TRIAGEM", id);
        startActivity(intent);
    }

    @Override
    public void onArquivarClick(int id) {
        Triagem t = encontrarTriagem(id);
        if (t != null && t.pulseira != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Arquivar")
                    .setMessage("Marcar como ATENDIDO e remover da lista?")
                    .setPositiveButton("Sim", (d, w) -> arquivarTriagemAPI(t))
                    .setNegativeButton("Não", null)
                    .show();
        } else {
            Toast.makeText(this, "Erro: Triagem sem pulseira.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEliminarClick(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Permanente")
                .setMessage("Tem a certeza? Isto apaga da Base de Dados.")
                .setPositiveButton("Sim, Apagar", (d, w) -> eliminarTriagemPermanente(id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private Triagem encontrarTriagem(int id) {
        for (Triagem t : listaTriagens) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    // API

    private void arquivarTriagemAPI(Triagem t) {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        // Rota da pulseira + ?arquivar=1
        String url = baseUrl + "api/pulseira/" + t.pulseira.getId() + "?auth_key=" + token + "&arquivar=1";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Arquivado com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarHistorico();
                },
                error -> {
                    Toast.makeText(this, "Erro ao arquivar.", Toast.LENGTH_SHORT).show();
                }
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
                // Enviar PUT
                params.put("_method", "PUT");
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void eliminarTriagemPermanente(int idTriagem) {
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String url = baseUrl + "api/triagem/" + idTriagem + "?auth_key=" + token;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Eliminado com sucesso.", Toast.LENGTH_SHORT).show();
                    carregarHistorico();
                },
                error -> Toast.makeText(this, "Erro ao eliminar.", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("_method", "DELETE");
                return params;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}