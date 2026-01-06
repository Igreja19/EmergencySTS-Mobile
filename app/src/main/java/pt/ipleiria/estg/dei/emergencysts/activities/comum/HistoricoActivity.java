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

        String role = SharedPrefManager.getInstance(this).getEnfermeiroBase().getRole();
        isPaciente = role != null && (role.equalsIgnoreCase("paciente") || role.equalsIgnoreCase("utente"));

        configurarInterface();

        btnBack.setOnClickListener(v -> finish());
        if (swipeRefreshLayout != null) swipeRefreshLayout.setOnRefreshListener(this::carregarHistorico);

        // Clique normal no item (para abrir detalhes)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Triagem t = listaTriagens.get(position);
            onTriagemClick(t.getId());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarHistorico();
    }

    private void configurarInterface() {
        tvTitulo.setText(isPaciente ? "O Meu Histórico" : "Histórico Geral");
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

                            System.out.println("DEBUG TRIAGEM: ID=" + t.getId() + " | Prioridade=" + prioridade + " | Status=" + status + " | IsPaciente=" + isPaciente);

                            if (prioridade.equalsIgnoreCase("Pendente")) continue;
                            // Esconde se for "Finalizado", "Concluída" ou "Atendido"
                            if (!isPaciente) {
                                if (status.equalsIgnoreCase("Finalizado") ||
                                        status.equalsIgnoreCase("Concluída") ||
                                        status.equalsIgnoreCase("Concluida")) {
                                    continue;
                                }
                            }

                            listaTriagens.add(t);
                        }

                        // PASSAR "this" (A ACTIVITY) COMO LISTENER
                        adapter = new TriagemAdapter(this, listaTriagens, !isPaciente, this);
                        listView.setAdapter(adapter);

                        if (tvTotalTriagens != null) tvTotalTriagens.setText("Total de triagens: " + listaTriagens.size());
                    } catch (Exception e) { e.printStackTrace(); }
                },
                error -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show();
                }
        );
        req.setShouldCache(false);
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    //  MÉTODOS DO LISTENER (Botões)

    @Override
    public void onTriagemClick(int id) {
        Intent intent = new Intent(this, DetalhesTriagemActivity.class);
        intent.putExtra("ID_TRIAGEM", id);
        startActivity(intent);
    }

    @Override
    public void onArquivarClick(int id) {
        Triagem t = encontrarTriagem(id);
        if (t != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Arquivar Triagem")
                    .setMessage("Quer esconder esta triagem da lista?")
                    .setPositiveButton("Sim", (d, w) -> arquivarTriagemAPI(t))
                    .setNegativeButton("Não", null)
                    .show();
        }
    }

    @Override
    public void onEliminarClick(int id) {
        Triagem t = encontrarTriagem(id);
        if (t != null) {
            new AlertDialog.Builder(this)
                    .setTitle("ELIMINAR PERMANENTE")
                    .setMessage("Tem a certeza? Isto apaga da Base de Dados.")
                    .setPositiveButton("Sim, Apagar", (d, w) -> eliminarTriagemPermanente(t.getId()))
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private Triagem encontrarTriagem(int id) {
        for (Triagem t : listaTriagens) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    //  API

    private void arquivarTriagemAPI(Triagem t) {
        // MUDANÇA: Adicionamos &arquivar=1 ao URL
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        // Adiciona "arquivar=1" para ativar o if($modoArquivar == '1') no PHP
        String url = baseUrl + "api/pulseira/" + t.pulseira.getId() + "?auth_key=" + token + "&arquivar=1";

        // MUDANÇA: Usamos POST com _method=PUT (Atualização) em vez de DELETE
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Arquivado com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarHistorico(); // Atualiza a lista
                },
                error -> {
                    Toast.makeText(this, "Erro ao arquivar: " + error.toString(), Toast.LENGTH_SHORT).show();
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
                //  MUDANÇA: Dizemos ao PHP que é um UPDATE (PUT)
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

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "Eliminado com sucesso.", Toast.LENGTH_SHORT).show();
                    carregarHistorico();
                },
                error -> Toast.makeText(this, "Erro ao eliminar.", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}