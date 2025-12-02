package pt.ipleiria.estg.dei.emergencysts.modelo;

public class Pulseira {
    private String id;
    private String prioridade;
    private String status;
    private String nome_paciente;
    private String hora;
    private String sns;

    public Pulseira(String id, String prioridade, String status, String nome_paciente, String sns, String hora) {
        this.id = id;
        this.prioridade = prioridade;
        this.status = status;
        this.nome_paciente = nome_paciente;
        this.hora = hora;
        this.sns = sns;
    }

    public String getId() {
        return id;
    }

    public String getPrioridade() {
        return prioridade;
    }

    public String getStatus() {
        return status;
    }

    public String getNomePaciente() {
        return nome_paciente;
    }
    public String getSns() { return sns; }
    public String getHora() { return hora; }

}
