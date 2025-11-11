package pt.ipleiria.estg.dei.emergencysts;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    // ⚙️ Endpoint da API (Yii2)
    private static final String URL_LOGIN = "http://10.0.2.2/platf/EmergencySTS/advanced/backend/web/api/auth/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.POST,
                URL_LOGIN,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean status = json.optBoolean("status", false);
                        String message = json.optString("message", "Erro");

                        if (status) {
                            JSONObject data = json.optJSONObject("data");
                            if (data != null) {
                                int userId = data.optInt("id", -1);
                                String role = data.optString("role", "paciente");
                                String authKey = data.optString("auth_key", "");
                                String nome = data.optString("username", "");

                                Toast.makeText(this, "Login efetuado com sucesso!", Toast.LENGTH_SHORT).show();

                                // 🔹 Decide para onde vai
                                Intent intent;
                                if (role.equalsIgnoreCase("enfermeiro")) {
                                    intent = new Intent(this, EnfermeiroActivity.class);
                                } else {
                                    intent = new Intent(this, PacienteActivity.class);
                                }

                                // 🔹 Passa os dados do utilizador
                                intent.putExtra("user_id", userId);
                                intent.putExtra("username", nome);
                                intent.putExtra("role", role);
                                intent.putExtra("auth_key", authKey);

                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erro ao processar resposta do servidor", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Erro de ligação ao servidor", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
