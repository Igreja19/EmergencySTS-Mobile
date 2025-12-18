package pt.ipleiria.estg.dei.emergencysts.activities.paciente;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Paciente;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class EditarPerfilPacienteActivity extends AppCompatActivity {

    private EditText etNome, etEmail, etTelefone, etMorada, etNif, etSns;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil_paciente);

        // Inicializar Views
        ImageView btnCancel = findViewById(R.id.btnCancel);
        Button btnSaveBottom = findViewById(R.id.btnSaveBottom);

        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etMorada = findViewById(R.id.etMorada);
        etNif = findViewById(R.id.etNif);
        etSns = findViewById(R.id.etSns);
        progressBar = findViewById(R.id.progressBar);

        // Preencher campos com dados atuais
        carregarDadosAtuais();

        // Ações dos botões
        btnCancel.setOnClickListener(v -> finish());
        btnSaveBottom.setOnClickListener(v -> guardarAlteracoes());
    }

    private void carregarDadosAtuais() {
        Paciente p = SharedPrefManager.getInstance(this).getPaciente();
        if (p != null) {
            etNome.setText(p.getNome());
            etEmail.setText(p.getEmail());
            etTelefone.setText(p.getTelefone());
            etMorada.setText(p.getMorada());
            etNif.setText(p.getNif());
            etSns.setText(p.getSns());
        }
    }

    private void guardarAlteracoes() {
        // Recolher dados dos campos de texto
        final String nome = etNome.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String telefone = etTelefone.getText().toString().trim();
        final String morada = etMorada.getText().toString().trim();
        final String nif = etNif.getText().toString().trim();
        final String sns = etSns.getText().toString().trim();

        // Validação básica
        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || morada.isEmpty() || nif.isEmpty() || sns.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        //  Preparar URL e Token
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        Paciente pacienteAtual = SharedPrefManager.getInstance(this).getPaciente();
        int idPaciente = pacienteAtual.getId();
        String token = SharedPrefManager.getInstance(this).getKeyAccessToken();

        String url = baseUrl + "api/paciente/" + idPaciente + "?auth_key=" + token;

        // Construir o JSON (Limpo)
        JSONObject jsonBody = new JSONObject();
        try {
            // Campos editáveis
            jsonBody.put("nome", nome);
            jsonBody.put("email", email);
            jsonBody.put("telefone", telefone);
            jsonBody.put("morada", morada);
            jsonBody.put("nif", nif);
            jsonBody.put("sns", sns);

            // Se por algum motivo for null, usa um fallback para não dar erro
            jsonBody.put("datanascimento", pacienteAtual.getDataNascimento() != null ? pacienteAtual.getDataNascimento() : "2000-01-01");
            jsonBody.put("genero", pacienteAtual.getGenero() != null ? pacienteAtual.getGenero() : "M");

        } catch (JSONException e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            return;
        }

        //  Enviar Pedido
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonBody,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();

                    // Atualizar os dados na App Localmente
                    pacienteAtual.setNome(nome);
                    pacienteAtual.setEmail(email);
                    pacienteAtual.setTelefone(telefone);
                    pacienteAtual.setMorada(morada);
                    pacienteAtual.setNif(nif);
                    pacienteAtual.setSns(sns);
                    SharedPrefManager.getInstance(this).savePaciente(pacienteAtual);

                    finish(); // Fecha a atividade e volta atrás
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    String mensagemErro = "Erro ao guardar alterações.";

                    // Tenta extrair a mensagem de erro específica do servidor (ex: "Email já existe")
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "UTF-8");

                            // Mostra uma mensagem mais amigável se possível, ou o erro cru
                            mensagemErro = "Erro do Servidor: " + error.networkResponse.statusCode;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void atualizarSharedPrefsLocalmente() {
        // MUDADO: Atualiza o objeto Paciente
        Paciente atual = SharedPrefManager.getInstance(this).getPaciente();
        if (atual != null) {
            atual.setNome(etNome.getText().toString());
            atual.setEmail(etEmail.getText().toString());
            atual.setTelefone(etTelefone.getText().toString());
            atual.setMorada(etMorada.getText().toString());
            atual.setNif(etNif.getText().toString());
            atual.setSns(etSns.getText().toString());

            SharedPrefManager.getInstance(this).savePaciente(atual);
        }
    }
}