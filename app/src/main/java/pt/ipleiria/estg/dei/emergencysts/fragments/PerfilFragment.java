package pt.ipleiria.estg.dei.emergencysts.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;
import java.util.Calendar;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.ConfigActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.EditarPerfilEnfermeiroActivity;
import pt.ipleiria.estg.dei.emergencysts.modelo.Enfermeiro;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PerfilFragment extends Fragment {

    private TextView tvNome, tvEmail, tvDataNasc, tvIdade, tvTelefone, tvSns, tvNif, tvMorada;
    private Button btnLogout;
    private ImageView btnBack, btnSettings, btnEditar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Insuflar o layout
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // 2. Ligar TODOS os componentes aos IDs do XML
        tvNome      = view.findViewById(R.id.tvNomeCompleto);
        tvEmail     = view.findViewById(R.id.tvEmail);
        tvDataNasc  = view.findViewById(R.id.tvDataNasc);
        tvIdade     = view.findViewById(R.id.tvIdade);
        tvTelefone  = view.findViewById(R.id.tvTelefone);
        tvSns       = view.findViewById(R.id.tvSns);
        tvNif       = view.findViewById(R.id.tvNif);
        tvMorada    = view.findViewById(R.id.tvMorada);

        // Botões (Certifica-te que estes IDs existem no fragment_perfil.xml)
        btnLogout   = view.findViewById(R.id.btnLogout);
        btnBack     = view.findViewById(R.id.btnBack);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnEditar   = view.findViewById(R.id.btnEditar);

        // 3. CONFIGURAR OS CLIQUES (Onde a magia acontece)

        // Botão Editar (Abre a atividade do teu colega)
        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> {
                // Tenta abrir a atividade
                try {
                    Intent intent = new Intent(getContext(), EditarPerfilEnfermeiroActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Erro ao abrir edição: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Debug: Se isto aparecer, o ID no XML está errado
            System.out.println("ERRO: Botão Editar não encontrado no XML!");
        }

        // Botão Logout (Mostra o Alerta)
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getContext() == null) return;

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Terminar Sessão");
                builder.setMessage("Tem a certeza que deseja sair?");

                builder.setPositiveButton("Sim", (dialog, which) -> {
                    SharedPrefManager.getInstance(getContext()).logout();
                });

                builder.setNegativeButton("Não", (dialog, which) -> {
                    dialog.dismiss();
                });

                builder.show();
            });
        }

        // Botão Voltar
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().finish();
            });
        }

        // Botão Settings (Config)
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(getContext(), ConfigActivity.class))
            );
        }

        // 4. Carregar Dados da API
        carregarPerfilEnfermeiro();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Atualiza os dados se tiveres voltado da edição
        carregarPerfilEnfermeiro();
    }

    private void carregarPerfilEnfermeiro() {
        if (getContext() == null) return;

        Enfermeiro stored = SharedPrefManager.getInstance(getContext()).getEnfermeiro();
        String token = SharedPrefManager.getInstance(getContext()).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(getContext()).getServerUrl();

        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String url = baseUrl + "api/enfermeiro/perfil?auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (getContext() == null) return;
                    try {
                        JSONObject data = response.has("data") ? response.optJSONObject("data") : response;
                        if (data == null) data = response;

                        String nome   = data.optString("nome", "---");
                        String email  = data.optString("email", "---");
                        String nasc   = data.optString("datanascimento", "---");
                        String tel    = data.optString("telefone", "---");
                        String sns    = data.optString("sns", "---");
                        String nif    = data.optString("nif", "---");
                        String morada = data.optString("morada", "---");

                        tvNome.setText(nome);
                        tvEmail.setText(email);
                        tvDataNasc.setText(nasc);
                        tvTelefone.setText(tel);
                        tvSns.setText(sns);
                        tvNif.setText(nif);
                        tvMorada.setText(morada);
                        tvIdade.setText(calcularIdade(nasc) + " anos");

                        // Atualiza Localmente
                        if (stored != null) {
                            stored.setNome(nome);
                            stored.setEmail(email);
                            stored.setDataNascimento(nasc);
                            stored.setTelefone(tel);
                            stored.setSns(sns);
                            stored.setNif(nif);
                            stored.setMorada(morada);
                            SharedPrefManager.getInstance(getContext()).saveEnfermeiro(stored);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> { /* Ignorar erros silenciosos */ }
        );

        VolleySingleton.getInstance(getContext()).addToRequestQueue(req);
    }

    private int calcularIdade(String data) {
        try {
            if (data == null || data.trim().isEmpty()) return 0;
            String[] p = data.split("-");
            if (p.length != 3) return 0;
            int ano = Integer.parseInt(p[0]);
            int mes = Integer.parseInt(p[1]);
            int dia = Integer.parseInt(p[2]);
            Calendar hoje = Calendar.getInstance();
            int idade = hoje.get(Calendar.YEAR) - ano;
            if (hoje.get(Calendar.MONTH) + 1 < mes || (hoje.get(Calendar.MONTH) + 1 == mes && hoje.get(Calendar.DAY_OF_MONTH) < dia))
                idade--;
            return idade;
        } catch (Exception ignored) { return 0; }
    }
}