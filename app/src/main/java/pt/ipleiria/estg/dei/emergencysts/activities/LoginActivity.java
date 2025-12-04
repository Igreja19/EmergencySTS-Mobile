package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest; // IMPORTANTE: Usar StringRequest

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

        // NÃO use navigateOnStart(this) aqui, pois cria um loop.
        // Use apenas a verificação se está logado:
        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            // Se já tem login, vai para a Main (Dashboard)
            // Assumi que tem um método openMainActivity ou pode chamar o Intent direto
            Intent intent = new Intent(this, EnfermeiroActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ImageView btnBack = findViewById(R.id.btnAtras);

        btnLogin.setOnClickListener(v -> loginUser());
        btnBack.setOnClickListener(v -> {
                Intent i = new Intent(LoginActivity.this, ConfigActivity.class);
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

        if (!SharedPrefManager.getInstance(this).hasServerConfigured()) {
            Toast.makeText(this, "Vai às configurações e mete o IP!", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. TÉCNICA 1: Enviar credenciais também no URL (Segurança extra para o Yii2 ler)
        String baseUrl = SharedPrefManager.getInstance(this).getServerUrl() + "api/auth/login";
        String url = baseUrl + "?username=" + username + "&password=" + password;

        // Usamos StringRequest porque é o formato "Formulário" nativo que o PHP adora
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        // O servidor respondeu! Vamos ver se é sucesso.
                        JSONObject json = new JSONObject(response);
                        boolean status = json.optBoolean("status", false);

                        if (status) {
                            JSONObject data = json.optJSONObject("data");
                            if (data != null) {
                                // SUCESSO!
                                int userId = data.optInt("user_id", -1);
                                if (userId == -1) userId = data.optInt("id", -1);

                                String token = data.optString("token");
                                if (token.isEmpty()) token = data.optString("access_token");
                                if (token.isEmpty()) token = data.optString("auth_key");

                                String role = data.optString("role", "paciente");
                                String email = data.optString("email", "");

                                if (!token.isEmpty()) {
                                    User user = new User(userId, username, email, role);
                                    SharedPrefManager.getInstance(this).userLogin(user, token);

                                    Toast.makeText(this, "Login efetuado!", Toast.LENGTH_SHORT).show();

                                    SharedPrefManager.getInstance(this).navigateOnStart(this);
                                    finish();
                                }
                            }
                        } else {
                            String msg = json.optString("message", "Credenciais Inválidas");
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao ler resposta JSON", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String erroMsg = "Erro de Ligação";
                    if (error.networkResponse != null) {
                        erroMsg = "Erro Servidor: " + error.networkResponse.statusCode;
                    }
                    Toast.makeText(this, erroMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // TÉCNICA 2: Enviar formato normal
                params.put("username", username);
                params.put("password", password);

                // TÉCNICA 3: Enviar formato Yii2 (LoginForm) - Este é o segredo!
                params.put("LoginForm[username]", username);
                params.put("LoginForm[password]", password);

                return params;
            }

            @Override
            public String getBodyContentType() {
                // Garante que vai como formulário
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}