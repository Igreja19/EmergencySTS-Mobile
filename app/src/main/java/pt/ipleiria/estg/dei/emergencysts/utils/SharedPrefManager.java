package pt.ipleiria.estg.dei.emergencysts.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import pt.ipleiria.estg.dei.emergencysts.activities.ConfigActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.EnfermeiroActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.LoginActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.PacienteActivity;
import pt.ipleiria.estg.dei.emergencysts.modelo.User;

public class SharedPrefManager {

    private static final String PREF_NAME = "emergencysts_pref";
    private static final String KEY_ACCESS_TOKEN = "key_access_token";

    // Chaves para os dados do Utilizador
    private static final String KEY_ID = "key_id";
    private static final String KEY_USERNAME = "key_username";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_ROLE = "key_role";

    private static final String KEY_SERVER_URL = "key_server_url";

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


    // -----------------------------
    // 🔐 LOGIN E UTILIZADORES
    // -----------------------------
    public void userLogin(User user, String accessToken) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(KEY_ID, user.getId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_ACCESS_TOKEN, accessToken);

        editor.apply();
    }

    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null) != null;
    }

    public User getUser() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new User(
                sharedPreferences.getInt(KEY_ID, -1),
                sharedPreferences.getString(KEY_USERNAME, null),
                sharedPreferences.getString(KEY_EMAIL, null),
                sharedPreferences.getString(KEY_ROLE, null)
        );
    }

    public String getKeyAccessToken() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public void logout() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);
    }


    // -----------------------------
    // 🌐 CONFIGURAÇÃO DO SERVIDOR
    // -----------------------------
    public void setServerUrl(String url) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public String getServerUrl() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        return sharedPreferences.getString(KEY_SERVER_URL,
                "http://10.0.2.2/platf/EmergencySTS/advanced/backend/web/");
    }

    public boolean hasServerConfigured() {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String url = sharedPreferences.getString(KEY_SERVER_URL, null);
        return url != null && !url.isEmpty();
    }


    // -----------------------------
    // 🚀 NOVO MÉTODO: NAVEGAÇÃO AO INICIAR A APP
    // -----------------------------
    public void navigateOnStart(Context context) {

        // 1️⃣ NÃO TEM SERVIDOR CONFIGURADO → IR PARA CONFIG
        if (!hasServerConfigured()) {
            Intent i = new Intent(context, ConfigActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
            return;
        }

        // 2️⃣ SERVIDOR OK, MAS UTILIZADOR NÃO LOGADO → LOGIN
        if (!isLoggedIn()) {
            Intent i = new Intent(context, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
            return;
        }

        // 3️⃣ UTILIZADOR LOGADO → ABRIR ATIVIDADE POR ROLE
        User user = getUser();
        Intent next;

        switch (user.getRole()) {
            case "enfermeiro":
                next = new Intent(context, EnfermeiroActivity.class);
                break;

            case "paciente":
                next = new Intent(context, PacienteActivity.class);
                break;

            default:
                next = new Intent(context, LoginActivity.class);
        }

        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(next);
    }
}
