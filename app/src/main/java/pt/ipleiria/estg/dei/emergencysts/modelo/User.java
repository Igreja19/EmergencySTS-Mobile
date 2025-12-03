package pt.ipleiria.estg.dei.emergencysts.modelo;

import android.os.Build;
import java.time.Year;

public class User {

    private int userId;
    private String username;
    private String email;
    private String role;

    private String nomeCompleto;
    private String dataNascimento;
    private String telefone;
    private String sns;
    private String nif;
    private String morada;

    // 🔹 Construtor antigo (NÃO pode ser removido — o login usa este!)
    public User(int userId, String username, String email, String role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    // 🔹 Construtor completo (opcional, continua disponível)
    public User(int userId, String username, String email, String role,
                String nomeCompleto, String dataNascimento,
                String telefone, String sns, String nif, String morada) {

        this(userId, username, email, role); // chama o básico primeiro

        this.nomeCompleto = nomeCompleto;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone;
        this.sns = sns;
        this.nif = nif;
        this.morada = morada;
    }

    // ---------- GETTERS ----------
    public int getId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public String getNomeCompleto() { return nomeCompleto; }
    public String getDataNascimento() { return dataNascimento; }
    public String getTelefone() { return telefone; }
    public String getSns() { return sns; }
    public String getNif() { return nif; }
    public String getMorada() { return morada; }

    public String getIdadeFormatada() {
        if (dataNascimento == null || dataNascimento.isEmpty()) return "-- anos";
        try {
            String[] parts = dataNascimento.split("/");
            int ano = Integer.parseInt(parts[2]);
            int atual = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? Year.now().getValue()
                    : 2025; // fallback
            return (atual - ano) + " anos";
        } catch (Exception e) {
            return "-- anos";
        }
    }

    // ---------- SETTERS (NOVOS!) ----------
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public void setDataNascimento(String dataNascimento) { this.dataNascimento = dataNascimento; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setSns(String sns) { this.sns = sns; }
    public void setNif(String nif) { this.nif = nif; }
    public void setMorada(String morada) { this.morada = morada; }
}
