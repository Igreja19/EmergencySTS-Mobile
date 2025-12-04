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
    private static final String KEY_ID = "key_id";
    private static final String KEY_USERNAME = "key_username";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_ROLE = "key_role";

    // 🌐 NOVOS CAMPOS PARA CONFIG
    private static final String KEY_SERVER_BASE = "key_server_base"; // apenas IP / domínio
    private static final String KEY_API_PATH = "key_api_path";       // caminho backend

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


    // -----------------------------------------------------
    // LOGIN & USER
    // -----------------------------------------------------

    public void userLogin(User user, String accessToken) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();

        e.putInt(KEY_ID, user.getId());
        e.putString(KEY_USERNAME, user.getUsername());
        e.putString(KEY_EMAIL, user.getEmail());
        e.putString(KEY_ROLE, user.getRole());
        e.putString(KEY_ACCESS_TOKEN, accessToken);

        e.apply();
    }

    public boolean isLoggedIn() {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_ACCESS_TOKEN, null) != null;
    }

    public User getUser() {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new User(
                sp.getInt(KEY_ID, -1),
                sp.getString(KEY_USERNAME, null),
                sp.getString(KEY_EMAIL, null),
                sp.getString(KEY_ROLE, null)
        );
    }

    public String getKeyAccessToken() {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_ACCESS_TOKEN, null);
    }

    public void logout() {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        Intent intent = new Intent(ctx, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ctx.startActivity(intent);
    }


    // -----------------------------------------------------
    // CONFIGURAÇÃO DO SERVIDOR
    // -----------------------------------------------------

    public void setServerBase(String baseUrl) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SERVER_BASE, baseUrl)
                .apply();
    }

    public void setApiPath(String path) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_API_PATH, path)
                .apply();
    }

    public String getServerBase() {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SERVER_BASE, "http://172.22.21.215");
    }

    public String getApiPath() {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API_PATH, "/EmergencySTS/advanced/backend/web/");
    }

    // URL COMPLETO FINAL GERADO AQUI
    public String getServerUrl() {
        return getServerBase() + getApiPath();
    }

    // Verificar se já existe configuração
    public boolean hasServerConfigured() {
        String base = getServerBase();
        return base != null && !base.isEmpty();
    }


    // -----------------------------------------------------
    // NAVEGAÇÃO AO INICIAR A APP
    // -----------------------------------------------------

    public void navigateOnStart(Context context) {

        // Não tem config → ConfigActivity
        if (!hasServerConfigured()) {
            Intent i = new Intent(context, ConfigActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
            return;
        }

        // Tem config mas não login → LoginActivity
        if (!isLoggedIn()) {
            Intent i = new Intent(context, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(i);
            return;
        }

        // 3️User logado → atividade por role
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
