package pt.ipleiria.estg.dei.emergencysts.activities.comum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONObject;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.DetalhesTriagemActivity;
import pt.ipleiria.estg.dei.emergencysts.adapters.TriagemAdapter;
import pt.ipleiria.estg.dei.emergencysts.listeners.TriagemListener;
import pt.ipleiria.estg.dei.emergencysts.modelo.Triagem;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;
import pt.ipleiria.estg.dei.emergencysts.utils.TriagemJsonParser;

// Implementamos TriagemListener para ouvir os cliques na lista
public class HistoricoActivity extends AppCompatActivity implements TriagemListener {

    private ListView listView;
    private TriagemAdapter adapter;
    private TextView tvTitulo, tvTotalTriagens;
    private ImageView btnBack;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isPaciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        // Inicializar Views
        tvTitulo = findViewById(R.id.tvTitulo);
        tvTotalTriagens = findViewById(R.id.tvTotalTriagens);
        btnBack = findViewById(R.id.btnBack);

        listView = findViewById(R.id.listViewTriagens);

        // SwipeRefresh
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        // Verificar quem está logado
        String role = SharedPrefManager.getInstance(this).getEnfermeiroBase().getRole();
        isPaciente = role != null && (role.equalsIgnoreCase("paciente") || role.equalsIgnoreCase("utente"));

        // Configurar UI
        configurarInterface();

        // Ações
        btnBack.setOnClickListener(v -> finish());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::carregarHistorico);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarHistorico();
    }

    private void configurarInterface() {
        if (isPaciente) {
            tvTitulo.setText("O Meu Histórico");
        } else {
            tvTitulo.setText("Histórico Geral");
        }
    }

    private void carregarHistorico() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // URL para pedir triagens (com expand para trazer dados extra)
        String url = baseUrl + "api/triagem?auth_key=" + token + "&expand=paciente,pulseira,userprofile";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    try {
                        ArrayList<Triagem> listaTriagens = new ArrayList<>();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            Triagem t = TriagemJsonParser.parserJsonTriagem(obj);

                            // Verifica a prioridade da pulseira
                            String prioridade = "Pendente"; // Valor por defeito
                            if (t.pulseira != null && t.pulseira.prioridade != null) {
                                prioridade = t.pulseira.prioridade;
                            }

                            // Só aparecem as que já têm cor
                            if (prioridade.equalsIgnoreCase("Pendente")) {
                                continue;
                            }

                            // Lógica de filtro por paciente (que já tínhamos)
                            if (isPaciente) {
                                listaTriagens.add(t);
                            } else {
                                listaTriagens.add(t);
                            }
                        }

                        // MUDANÇA CRÍTICA: O teu Adapter não tem método "setTriagens" e a lista é final.
                        // Solução para BaseAdapter: Criar um novo adapter com a nova lista.
                        adapter = new TriagemAdapter(this, listaTriagens, this);
                        listView.setAdapter(adapter);

                        // Atualizar contador
                        if (tvTotalTriagens != null) {
                            tvTotalTriagens.setText("Total de triagens: " + listaTriagens.size());
                        }

                        if (listaTriagens.isEmpty()) {
                            Toast.makeText(this, "Sem registos.", Toast.LENGTH_SHORT).show();
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

        req.setShouldCache(false); // Importante para veres o "pixamole"
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // Implementação do método da interface TriagemListener
    @Override
    public void onTriagemClick(int id) {
        // Ao clicar num item da lista
        if (!isPaciente) {
            // Se for enfermeiro, abre os detalhes
            Intent intent = new Intent(this, DetalhesTriagemActivity.class);
            intent.putExtra("ID_TRIAGEM", id);
            startActivity(intent);
        } else {
            // Se for paciente, talvez queiras abrir detalhes ou não fazer nada
            // Toast.makeText(this, "Triagem ID: " + id, Toast.LENGTH_SHORT).show();
        }
    }
}