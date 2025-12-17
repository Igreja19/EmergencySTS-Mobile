package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
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
    private ImageView btnEditar;

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

        btnEditar   = findViewById(R.id.btnEditar);
        btnLogout   = findViewById(R.id.btnLogout);

        // Botão voltar
        btnBack.setOnClickListener(v -> finish());

        // Carregar dados da API (para garantir que temos os dados mais recentes do servidor)
        carregarPerfilPaciente();

        // Abrir página de editar
        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(PerfilPacienteActivity.this, EditarPerfilPacienteActivity.class);
            startActivity(intent);
        });

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

    // --- CORREÇÃO DO BUG DE REFRESH ---
    @Override
    protected void onResume() {
        super.onResume();
        // Sempre que a janela aparece (ou volta de editar), atualiza os textos
        // com o que está guardado localmente nas SharedPreferences.
        atualizarDadosInterface();
    }

    // Método auxiliar para preencher os TextViews com os dados locais
    private void atualizarDadosInterface() {
        Paciente p = SharedPrefManager.getInstance(this).getPaciente();

        if (p != null) {
            tvNome.setText(p.getNome());
            tvEmail.setText(p.getEmail());
            tvDataNasc.setText(p.getDataNascimento());
            tvTelefone.setText(p.getTelefone());
            tvSns.setText(p.getSns());
            tvNif.setText(p.getNif());
            tvMorada.setText(p.getMorada());

            // Recalcular idade baseado na data guardada
            tvIdade.setText(calcularIdade(p.getDataNascimento()) + " anos");
        }
    }

    //          CARREGAR PERFIL DO PACIENTE LOGADO DA API
    private void carregarPerfilPaciente() {

        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String url = baseUrl + "api/paciente/perfil?auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONObject data = response;

                        String nome   = data.optString("nome", "---");
                        String email  = data.optString("email", "---");
                        String nasc   = data.optString("datanascimento", "---");
                        String tel    = data.optString("telefone", "---");
                        String sns    = data.optString("sns", "---");
                        String nif    = data.optString("nif", "---");
                        String morada = data.optString("morada", "---");

                        // --- CORREÇÃO DE OBJETO ---
                        // Estavas a usar .getEnfermeiro().getId(), o que dava erro se for paciente.
                        // Vamos buscar os dados base do Paciente atual.
                        Paciente currentP = SharedPrefManager.getInstance(this).getPaciente();
                        int userId = currentP.getId();
                        String username = currentP.getUsername();

                        // Criar objeto atualizado
                        Paciente pUpdated = new Paciente(
                                userId,
                                username,
                                email,
                                "paciente",
                                nome,
                                nasc,
                                tel,
                                sns,
                                nif,
                                morada
                        );

                        // 1. Guardar localmente
                        SharedPrefManager.getInstance(this).savePaciente(pUpdated);

                        // 2. Atualizar o ecrã
                        atualizarDadosInterface();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Se falhar a net, pelo menos mostramos o que temos guardado
                    atualizarDadosInterface();
                    Toast.makeText(this, "Erro ao atualizar perfil.", Toast.LENGTH_SHORT).show();
                }
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