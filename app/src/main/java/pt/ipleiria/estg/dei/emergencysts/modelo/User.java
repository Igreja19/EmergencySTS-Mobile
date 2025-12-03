package pt.ipleiria.estg.dei.emergencysts.modelo;

public class User {
    private int id;
    private String username;
    private String email;
    private String role; // "admin", "medico", "enfermeiro", "utente"

    public User(int id, String username, String email, String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}