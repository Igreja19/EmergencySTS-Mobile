package pt.ipleiria.estg.dei.emergencysts.modelo;

public class Paciente {
    private int id;
    private String nome;
    private String nif;
    private String sns;
    private String dataNascimento;

    public Paciente(int id, String nome, String nif, String sns, String dataNascimento) {
        this.id = id;
        this.nome = nome;
        this.nif = nif;
        this.sns = sns;
        this.dataNascimento = dataNascimento;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getNif() { return nif; }
    public String getSns() { return sns; }
    public String getDataNascimento() { return dataNascimento; }
}