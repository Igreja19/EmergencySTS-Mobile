package pt.ipleiria.estg.dei.emergencysts.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.EnfermeiroActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.paciente.PacienteActivity;
import pt.ipleiria.estg.dei.emergencysts.listeners.LoginListener;
import pt.ipleiria.estg.dei.emergencysts.modelo.Enfermeiro;
import pt.ipleiria.estg.dei.emergencysts.mqtt.MqttClientManager;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class LoginActivity extends AppCompatActivity implements LoginListener {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            Enfermeiro user = SharedPrefManager.getInstance(this).getEnfermeiroBase();
            if (user != null) {
                navegarParaActivity(user.getRole(), user.getUsername());
                return;
            }
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        ImageView btnConfig = findViewById(R.id.btnAtras);

        VolleySingleton.getInstance(this).setLoginListener(this);

        btnLogin.setOnClickListener(v -> loginUser());

        btnConfig.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        VolleySingleton.getInstance(this).setLoginListener(this);
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            etUsername.setError("Preencha tudo");
            return;
        }

        // Bloquear botão
        btnLogin.setEnabled(false);
        btnLogin.setText("A verificar...");

        VolleySingleton.getInstance(this).loginAPI(username, password);
    }

    @Override
    public void onValidateLogin(String token, String username, String role) {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        navegarParaActivity(role, username);
    }

    @Override
    public void onLoginError(String error) {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    private void navegarParaActivity(String rawRole, String username) {
        try {
            MqttClientManager.getInstance(this).connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String role = (rawRole == null) ? "Enfermeiro" : rawRole.trim();
        Intent intent;

        if (role.equalsIgnoreCase("paciente") || role.equalsIgnoreCase("utente")) {
            if (username != null && !username.isEmpty()) {
                Toast.makeText(this, "Bem-vindo " + username, Toast.LENGTH_SHORT).show();
            }
            intent = new Intent(this, PacienteActivity.class);
        }
        else if (role.equalsIgnoreCase("medico")) {
            SharedPrefManager.getInstance(this).logout();
            Toast.makeText(this, "Médicos usam o painel Web.", Toast.LENGTH_LONG).show();
            return;
        }
        else if (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("administrador")) {
            SharedPrefManager.getInstance(this).logout();
            Toast.makeText(this, "Administradores usem o painel Web.", Toast.LENGTH_LONG).show();
            return;
        }
        else {
            if (username != null && !username.isEmpty()) {
                Toast.makeText(this, "Bem-vindo " + username, Toast.LENGTH_SHORT).show();
            }
            intent = new Intent(this, EnfermeiroActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}