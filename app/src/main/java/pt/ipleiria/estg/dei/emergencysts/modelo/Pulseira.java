package pt.ipleiria.estg.dei.emergencysts.modelo;

public class Pulseira {
    // Campos Básicos
    private String id;
    private String prioridade;
    private String status;
    private String nomePaciente;
    private String sns;
    private String hora;

    // Novos Campos (Para o ecrã de Atribuir)
    private String dataNascimento;
    private String telefone;
    private String motivo;
    private String queixa;
    private String descricao;
    private String inicioSintomas;
    private String dor;
    private String alergias;
    private String medicacao;

    // Construtor Completo
    public Pulseira(String id, String prioridade, String status, String nomePaciente, String sns, String hora,
                    String dataNascimento, String telefone, String motivo, String queixa, String descricao,
                    String inicioSintomas, String dor, String alergias, String medicacao) {
        this.id = id;
        this.prioridade = prioridade;
        this.status = status;
        this.nomePaciente = nomePaciente;
        this.sns = sns;
        this.hora = hora;
        this.dataNascimento = dataNascimento;
        this.telefone = telefone;
        this.motivo = motivo;
        this.queixa = queixa;
        this.descricao = descricao;
        this.inicioSintomas = inicioSintomas;
        this.dor = dor;
        this.alergias = alergias;
        this.medicacao = medicacao;
    }

    // Getters
    public String getId() { return id; }
    public String getPrioridade() { return prioridade; }
    public String getStatus() { return status; }
    public String getNomePaciente() { return nomePaciente; }
    public String getSns() { return sns; }
    public String getHora() { return hora; }

    // Getters dos Novos Campos
    public String getDataNascimento() { return dataNascimento; }
    public String getTelefone() { return telefone; }
    public String getMotivo() { return motivo; }
    public String getQueixa() { return queixa; }
    public String getDescricao() { return descricao; }
    public String getInicioSintomas() { return inicioSintomas; }
    public String getDor() { return dor; }
    public String getAlergias() { return alergias; }
    public String getMedicacao() { return medicacao; }
}