package pt.ipleiria.estg.dei.emergencysts.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <--- Importante
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.Calendar;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Enfermeiro;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PerfilFragment extends Fragment {

    private TextView tvNome, tvEmail, tvDataNasc, tvIdade, tvTelefone, tvSns, tvNif, tvMorada;
    private ImageView btnBack;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // Ligar TODOS os componentes
        tvNome      = view.findViewById(R.id.tvNomeCompleto);
        tvEmail     = view.findViewById(R.id.tvEmail);
        tvDataNasc  = view.findViewById(R.id.tvDataNasc);
        tvIdade     = view.findViewById(R.id.tvIdade);
        tvTelefone  = view.findViewById(R.id.tvTelefone);
        tvSns       = view.findViewById(R.id.tvSns);
        tvNif       = view.findViewById(R.id.tvNif);
        tvMorada    = view.findViewById(R.id.tvMorada);

        btnBack     = view.findViewById(R.id.btnBack);
        btnLogout   = view.findViewById(R.id.btnLogout);

        // Configurar botão de voltar
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().finish();
            });
        }

        // Configurar botão de LOGOUT
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getContext() != null) {
                    SharedPrefManager.getInstance(getContext()).logout();
                }
            });
        }

        // Carregar dados
        carregarPerfil();

        return view;
    }

    private void carregarPerfil() {
        if (getContext() == null) return;

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

                        // Preencher UI
                        tvNome.setText(nome);
                        tvEmail.setText(email);
                        tvDataNasc.setText(nasc);
                        tvTelefone.setText(tel);
                        tvSns.setText(sns);
                        tvNif.setText(nif);
                        tvMorada.setText(morada);
                        tvIdade.setText(calcularIdade(nasc) + " anos");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Erro ao carregar perfil", Toast.LENGTH_SHORT).show();
                }
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