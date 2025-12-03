package pt.ipleiria.estg.dei.emergencysts.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import pt.ipleiria.estg.dei.emergencysts.activities.LoginActivity;
import pt.ipleiria.estg.dei.emergencysts.modelo.User;

public class SharedPrefManager {

    private static final String PREF_NAME = "emergencysts_pref";
    private static final String KEY_ACCESS_TOKEN = "key_access_token";

    // Chaves para os dados do Utilizador
    private static final String KEY_ID = "key_id";
    private static final String KEY_USERNAME = "key_username";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_ROLE = "key_role";

    private static SharedPrefManager instance;
    private static Context ctx;

    private SharedPrefManager(Context context) {
        ctx = context;
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    // --- MÉTODOS DE LOGIN ---

    /**
     * Guarda os dados do utilizador (Objeto User) e o token
     */
    public void userLogin(User user, String accessToken) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(KEY_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_ACCESS_TOKEN, accessToken);

        editor.apply(); // 'apply' é assíncrono e mais rápido que 'commit'
    }

    /**
     * Verifica se o utilizador está logado (se tem token)
     */
    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null) != null;
    }

    /**
     * Recupera o Utilizador logado como um Objeto User
     */
    public User getUser() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new User(
                sharedPreferences.getInt(KEY_ID, -1),
                sharedPreferences.getString(KEY_USERNAME, null),
                sharedPreferences.getString(KEY_EMAIL, null),
                sharedPreferences.getString(KEY_ROLE, null)
        );
    }

    /**
     * Devolve apenas o Token (para cabeçalhos de API)
     */
    public String getKeyAccessToken() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Faz Logout: limpa os dados e redireciona para o LoginActivity
     */
    public void logout() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Redireciona para o Login e limpa o histórico de navegação
        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);
    }

    // --- ENDEREÇO DO SERVIDOR ---

    // Podes manter o teu método dinâmico se preferires,
    // mas aqui fica configurado para o IP que usaste anteriormente.
    public String getServerUrl() {
        // IP que usaste noutros ficheiros (Atualiza se mudar!)
        return "http://10.0.2.2/EmergencySTS/advanced/backend/web/";
    }
}