package pt.ipleiria.estg.dei.emergencysts.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;
import java.util.Calendar;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.activities.auth.ConfigActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.enfermeiro.EditarPerfilEnfermeiroActivity;
import pt.ipleiria.estg.dei.emergencysts.activities.paciente.EditarPerfilPacienteActivity;
import pt.ipleiria.estg.dei.emergencysts.modelo.Enfermeiro;
import pt.ipleiria.estg.dei.emergencysts.modelo.Paciente;
import pt.ipleiria.estg.dei.emergencysts.network.VolleySingleton;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class PerfilFragment extends Fragment {

    private TextView tvNome, tvEmail, tvDataNasc, tvIdade, tvTelefone, tvSns, tvNif, tvMorada;
    private Button btnLogout;
    private ImageView btnBack, btnSettings, btnEditar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //  Insuflar o layout
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        //  Ligar TODOS os componentes aos IDs do XML
        tvNome      = view.findViewById(R.id.tvNomeCompleto);
        tvEmail     = view.findViewById(R.id.tvEmail);
        tvDataNasc  = view.findViewById(R.id.tvDataNasc);
        tvIdade     = view.findViewById(R.id.tvIdade);
        tvTelefone  = view.findViewById(R.id.tvTelefone);
        tvSns       = view.findViewById(R.id.tvSns);
        tvNif       = view.findViewById(R.id.tvNif);
        tvMorada    = view.findViewById(R.id.tvMorada);

        // Botões (Certifica-te que estes IDs existem no fragment_perfil.xml)
        btnLogout   = view.findViewById(R.id.btnLogout);
        btnBack     = view.findViewById(R.id.btnBack);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnEditar   = view.findViewById(R.id.btnEditar);

        //  CONFIGURAR OS CLIQUES

        // Botão Editar
        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> {
                String role = SharedPrefManager.getInstance(getContext()).getEnfermeiroBase().getRole();
                Intent intent;

                // Abre a atividade de edição consoante o tipo de utilizador
                if (role != null && (role.equalsIgnoreCase("paciente"))) {
                    intent = new Intent(getContext(), EditarPerfilPacienteActivity.class);
                } else {
                    intent = new Intent(getContext(), EditarPerfilEnfermeiroActivity.class);
                }
                startActivity(intent);
            });
        }

        // Botão Logout (Mostra o Alerta)
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getContext() == null) return;

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Terminar Sessão");
                builder.setMessage("Tem a certeza que deseja sair?");

                builder.setPositiveButton("Sim", (dialog, which) -> {
                    SharedPrefManager.getInstance(getContext()).logout();
                });

                builder.setNegativeButton("Não", (dialog, which) -> {
                    dialog.dismiss();
                });

                builder.show();
            });
        }

        // Botão Voltar
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().finish();
            });
        }

        // Botão Settings (Config)
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(getContext(), ConfigActivity.class))
            );
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getContext() == null) return;

        String role = SharedPrefManager.getInstance(getContext()).getEnfermeiroBase().getRole();

        // Se for Paciente carrega paciente
        if (role != null && (role.equalsIgnoreCase("paciente"))) {
            carregarPerfilPaciente();
        } else {
            // Caso contrário (enfermeiro ou admin)
            carregarPerfilEnfermeiro();
        }
    }

    private void carregarPerfilEnfermeiro() {
        if (getContext() == null) return;

        Enfermeiro stored = SharedPrefManager.getInstance(getContext()).getEnfermeiro();
        String token = SharedPrefManager.getInstance(getContext()).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(getContext()).getServerUrl();

        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String url = baseUrl + "api/enfermeiro/perfil?auth_key=" + token;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (getContext() == null) return;
                    try {
                        JSONObject data = response.has("data") ? response.optJSONObject("data") : response;
                        if (data == null) data = response;

                        String nome   = data.optString("nome", "---");
                        String email  = data.optString("email", "---");
                        String nasc   = data.optString("datanascimento", "---");
                        String tel    = data.optString("telefone", "---");
                        String sns    = data.optString("sns", "---");
                        String nif    = data.optString("nif", "---");
                        String morada = data.optString("morada", "---");

                        tvNome.setText(nome);
                        tvEmail.setText(email);
                        tvDataNasc.setText(nasc);
                        tvTelefone.setText(tel);
                        tvSns.setText(sns);
                        tvNif.setText(nif);
                        tvMorada.setText(morada);
                        tvIdade.setText(calcularIdade(nasc) + " anos");

                        // Atualiza Localmente
                        if (stored != null) {
                            stored.setNome(nome);
                            stored.setEmail(email);
                            stored.setDataNascimento(nasc);
                            stored.setTelefone(tel);
                            stored.setSns(sns);
                            stored.setNif(nif);
                            stored.setMorada(morada);
                            SharedPrefManager.getInstance(getContext()).saveEnfermeiro(stored);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> { /* Ignorar erros silenciosos */ }
        );

        VolleySingleton.getInstance(getContext()).addToRequestQueue(req);
    }

    private void carregarPerfilPaciente() {
        if (getContext() == null) return;

        // Preparar dados
        String token = SharedPrefManager.getInstance(getContext()).getKeyAccessToken();
        String baseUrl = SharedPrefManager.getInstance(getContext()).getServerUrl();
        int userIdLogado = SharedPrefManager.getInstance(getContext()).getEnfermeiroBase().getId();

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // Pedir a LISTA de pacientes
        String url = baseUrl + "api/paciente?auth_key=" + token;

        // Usamos JsonArrayRequest porque usamos uma lista
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (getContext() == null) return;
                    try {
                        boolean encontrado = false;

                        // Procurar o paciente que corresponde ao User ID logado
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject data = response.getJSONObject(i);
                            int uId = data.optInt("user_id", -1);

                            if (uId == userIdLogado) {
                                encontrado = true;

                                // Extrair dados
                                int idPac     = data.optInt("id");
                                String nome   = data.optString("nome", "---");
                                String email  = data.optString("email", "---");
                                String nasc   = data.optString("datanascimento", "---");
                                String tel    = data.optString("telefone", "---");
                                String sns    = data.optString("sns", "---");
                                String nif    = data.optString("nif", "---");
                                String morada = data.optString("morada", "---");
                                String genero = data.optString("genero", "M");

                                // Atualizar UI
                                tvNome.setText(nome);
                                tvEmail.setText(email);
                                tvDataNasc.setText(nasc);
                                tvTelefone.setText(tel);
                                tvSns.setText(sns);
                                tvNif.setText(nif);
                                tvMorada.setText(morada);
                                tvIdade.setText(calcularIdade(nasc) + " anos");

                                //  Atualizar/Guardar no SharedPrefManager
                                Paciente stored = SharedPrefManager.getInstance(getContext()).getPaciente();
                                // Se não existir ou o ID for diferente, criamos um novo com o ID certo
                                if (stored == null || stored.getId() != idPac) {
                                    stored = new Paciente(idPac, "user", email, "paciente");
                                }

                                stored.setNome(nome);
                                stored.setEmail(email);
                                stored.setDataNascimento(nasc);
                                stored.setTelefone(tel);
                                stored.setSns(sns);
                                stored.setNif(nif);
                                stored.setMorada(morada);
                                stored.setGenero(genero);

                                SharedPrefManager.getInstance(getContext()).savePaciente(stored);
                                break; // Já encontrámos, podemos sair do loop
                            }
                        }

                        if (!encontrado) {
                            tvNome.setText("Perfil não encontrado");
                            Toast.makeText(getContext(), "Não foi encontrado um perfil de paciente associado.", Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Erro ao processar dados.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Agora mostramos o erro para não ficar "A Carregar..." para sempre
                    if (getContext() != null) {
                        tvNome.setText("Erro de Ligação");
                        String err = error.getMessage() != null ? error.getMessage() : "Erro desconhecido";
                        Toast.makeText(getContext(), "Erro ao carregar: " + err, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        VolleySingleton.getInstance(getContext()).addToRequestQueue(req);
    }

    private int calcularIdade(String data) {
        try {
            if (data == null || data.trim().isEmpty()) return 0;
            String[] p = data.split("-");
            if (p.length != 3) return 0;
            int ano = Integer.parseInt(p[0]);
            int mes = Integer.parseInt(p[1]);
            int dia = Integer.parseInt(p[2]);
            Calendar hoje = Calendar.getInstance();
            int idade = hoje.get(Calendar.YEAR) - ano;
            if (hoje.get(Calendar.MONTH) + 1 < mes || (hoje.get(Calendar.MONTH) + 1 == mes && hoje.get(Calendar.DAY_OF_MONTH) < dia))
                idade--;
            return idade;
        } catch (Exception ignored) { return 0; }
    }
}