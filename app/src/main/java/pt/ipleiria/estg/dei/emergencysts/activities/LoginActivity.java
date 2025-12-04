package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.User;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Se já tiver token, vai direto para a Activity correta sem pedir pass
        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            User user = SharedPrefManager.getInstance(this).getUser();
            redirecionarPorRole(user.getRole());
            return;
        }


        ImageView btnBack = findViewById(R.id.btnBack);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);


        btnLogin.setOnClickListener(v -> loginUser());
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(this, ConfigActivity.class);
            startActivity(i);
            finish();
        });
    }

    private void loginUser() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = SharedPrefManager.getInstance(this).getServerUrl() + "api/auth/login";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean status = json.optBoolean("status", false);

                        if (status) {
                            // A tua API devolve os dados dentro de "data"
                            JSONObject data = json.optJSONObject("data");

                            if (data != null) {
                                // 1. LER DADOS (Mantendo a tua lógica robusta)
                                int userId = data.optInt("user_id", -1);
                                if (userId == -1) userId = data.optInt("id", -1);

                                String token = data.optString("token");
                                if (token.isEmpty()) token = data.optString("access_token");
                                if (token.isEmpty()) token = data.optString("auth_key");

                                String role = data.optString("role", "paciente");
                                String email = data.optString("email", ""); // Se a API enviar email

                                if (token.isEmpty()) {
                                    Toast.makeText(this, "Erro: Token inválido", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // 2. CRIAR OBJETO USER (Isto é o que muda!)
                                // Estamos a criar o objeto para guardar de forma estruturada
                                User user = new User(userId, username, email, role);

                                // 3. GUARDAR SESSÃO (Usando o novo método userLogin)
                                SharedPrefManager.getInstance(this).userLogin(user, token);

                                Toast.makeText(this, "Bem-vindo " + username + "!", Toast.LENGTH_SHORT).show();

                                // 4. REDIRECIONAR
                                redirecionarPorRole(role);
                            }
                        } else {
                            String msg = json.optString("message", "Dados incorretos"); // Tenta ler msg de erro da API
                            if (msg.equals("null")) msg = "Credenciais Inválidas"; // Fallback visual
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar resposta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Erro de ligação ao servidor", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void redirecionarPorRole(String role) {
        Intent intent;
        if (role != null && (role.equalsIgnoreCase("enfermeiro") || role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("medico"))) {
            intent = new Intent(this, EnfermeiroActivity.class);
        } else {
            intent = new Intent(this, PacienteActivity.class);
        }
        startActivity(intent);
        finish(); // Fecha o Login para não voltar para trás
    }
}