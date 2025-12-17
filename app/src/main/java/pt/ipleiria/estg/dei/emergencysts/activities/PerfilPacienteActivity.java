package pt.ipleiria.estg.dei.emergencysts.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.Calendar;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Paciente;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PerfilPacienteActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvNome, tvEmail, tvDataNasc, tvIdade, tvTelefone, tvSns, tvNif, tvMorada;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_paciente);

        // Ligação ao XML
        btnBack     = findViewById(R.id.btnBack);

        tvNome      = findViewById(R.id.tvNomeCompleto);
        tvEmail     = findViewById(R.id.tvEmail);
        tvDataNasc  = findViewById(R.id.tvDataNasc);
        tvIdade     = findViewById(R.id.tvIdade);
        tvTelefone  = findViewById(R.id.tvTelefone);
        tvSns       = findViewById(R.id.tvSns);
        tvNif       = findViewById(R.id.tvNif);
        tvMorada    = findViewById(R.id.tvMorada);

        btnLogout   = findViewById(R.id.btnLogout);

        // Botão voltar
        btnBack.setOnClickListener(v -> finish());

        // Carregar perfil do paciente logado
        carregarPerfilPaciente();

        // Logout
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Terminar sessão")
                .setMessage("Tem a certeza que deseja sair?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    SharedPrefManager.getInstance(this).logout();
                })
                .setNegativeButton("Não", null)
                .show();
        });
    }

    //          CARREGAR PERFIL DO PACIENTE LOGADO
    private void carregarPerfilPaciente() {

        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // ENDPOINT CORRETO — devolve exatamente 1 paciente
        String url = baseUrl + "api/paciente/perfil?auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONObject data = response; // endpoint devolve objeto único

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

                        // Atualizar o objeto Paciente local
                        Paciente p = new Paciente(
                                SharedPrefManager.getInstance(this).getEnfermeiro().getId(),
                                SharedPrefManager.getInstance(this).getEnfermeiro().getUsername(),
                                email,
                                "paciente",
                                nome,
                                nasc,
                                tel,
                                sns,
                                nif,
                                morada
                        );

                        SharedPrefManager.getInstance(this).savePaciente(p);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar dados do perfil.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erro ao comunicar com o servidor.", Toast.LENGTH_SHORT).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    //          CALCULAR IDADE (YYYY-MM-DD)
    private int calcularIdade(String data) {
        try {
            if (data == null || data.trim().isEmpty()) return 0;

            String[] p = data.split("-");
            if (p.length != 3) return 0;

            int ano = Integer.parseInt(p[0]);
            int mes = Integer.parseInt(p[1]);
            int dia = Integer.parseInt(p[2]);

            Calendar hoje = Calendar.getInstance();

            int anoAtual = hoje.get(Calendar.YEAR);
            int mesAtual = hoje.get(Calendar.MONTH) + 1;
            int diaAtual = hoje.get(Calendar.DAY_OF_MONTH);

            int idade = anoAtual - ano;

            if (mesAtual < mes || (mesAtual == mes && diaAtual < dia)) {
                idade--;
            }

            return idade;

        } catch (Exception ignored) {
            return 0;
        }
    }
}
