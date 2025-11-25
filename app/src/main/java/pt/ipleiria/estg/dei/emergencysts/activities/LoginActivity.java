package pt.ipleiria.estg.dei.emergencysts.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);

        // Define o URL base do servidor (para o emulador é 10.0.2.2)
        SharedPrefManager.getInstance(this).saveServerUrl("http://10.0.2.2/EmergencySTS/advanced/backend/web/");

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        // Obtemos os textos dos campos
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        // Validação simples
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Montar o URL
        String url = SharedPrefManager.getInstance(this).getServerUrl() + "api/auth/login";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        // DEBUG: Para ver no Logcat o que o servidor respondeu
                        System.out.println("LOGIN RESPOSTA: " + response);

                        JSONObject json = new JSONObject(response);
                        boolean status = json.optBoolean("status", false);

                        if (status) {
                            JSONObject data = json.optJSONObject("data");

                            if (data != null) {
                                // CORREÇÃO 1: O seu JSON usa "user_id", não "id"
                                int userId = data.optInt("user_id", -1);
                                if (userId == -1) {
                                    userId = data.optInt("id", -1); // Fallback
                                }

                                // CORREÇÃO 2: O seu JSON usa "token", não "access_token"
                                String accessToken = data.optString("token");

                                // Caso a API mude no futuro, mantemos estas tentativas:
                                if (accessToken.isEmpty()) {
                                    accessToken = data.optString("access_token");
                                }
                                if (accessToken.isEmpty()) {
                                    accessToken = data.optString("auth_key");
                                }

                                String role = data.optString("role", "paciente");

                                // Validação do Token
                                if (accessToken.isEmpty()) {
                                    Toast.makeText(this, "Erro: Token não encontrado na resposta!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                System.out.println("TOKEN GUARDADO: " + accessToken);

                                // Guardar na memória do telemóvel
                                SharedPrefManager.getInstance(this).saveUser(userId, username, role, accessToken);

                                Toast.makeText(this, "Login efetuado com sucesso!", Toast.LENGTH_SHORT).show();

                                // Redirecionar
                                Intent intent;
                                if (role.equalsIgnoreCase("enfermeiro") || role.equalsIgnoreCase("admin")) {
                                    intent = new Intent(this, EnfermeiroActivity.class);
                                } else {
                                    // Certifique-se que tem a PacienteActivity criada
                                    intent = new Intent(this, PacienteActivity.class);
                                }
                                startActivity(intent);
                                finish(); // Fecha o Login para não voltar atrás
                            }
                        } else {
                            String message = json.optString("message", "Credenciais Inválidas");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Trata erro de rede
                    String errMessage = "Erro de ligação ao servidor";
                    if (error.networkResponse != null) {
                        errMessage += " (Code " + error.networkResponse.statusCode + ")";
                    }
                    Toast.makeText(this, errMessage, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}