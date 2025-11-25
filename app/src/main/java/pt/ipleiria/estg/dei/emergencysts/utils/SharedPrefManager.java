package pt.ipleiria.estg.dei.emergencysts.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String PREF_NAME = "emergency_prefs";
    private static final String KEY_SERVER = "server_url";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    private static SharedPrefManager instance;
    private final SharedPreferences prefs;

    // 🔹 Construtor privado (padrão Singleton)
    private SharedPrefManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 🔹 Método estático para aceder à mesma instância (Singleton)
    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    // 🔹 Guarda dados do utilizador autenticado
    public void saveUser(int id, String username, String role, String accessToken) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, id);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.commit();
    }

    // 🔹 Guarda o endereço do servidor (WAMP / API)
    public void saveServerUrl(String url) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER, url);
        editor.commit();
    }

    // 🔹 Obtém o endereço do servidor
    public String getServerUrl() {
        // valor por defeito: o teu WAMP local
        return prefs.getString(KEY_SERVER, "http://10.0.2.2/EmergencySTS/advanced/backend/web/");
    }

    // 🔹 Getters
    public int getUserId() { return prefs.getInt(KEY_USER_ID, -1); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, ""); }
    public String getRole() { return prefs.getString(KEY_ROLE, ""); }
    public String getKeyAccessToken() { return prefs.getString(KEY_ACCESS_TOKEN, ""); }

    // 🔹 Verifica se existe utilizador autenticado
    public boolean isLoggedIn() {
        return getUserId() != -1 && !getKeyAccessToken().isEmpty();
    }

    // 🔹 Limpa tudo (logout)
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    public String getAuthKey() {
        return getKeyAccessToken();
    }
}
