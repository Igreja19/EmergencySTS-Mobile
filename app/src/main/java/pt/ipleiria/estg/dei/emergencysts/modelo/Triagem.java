package pt.ipleiria.estg.dei.emergencysts.modelo;

public class Triagem {

    // CAMPOS PRINCIPAIS DA TRIAGEM
    public int id;
    public String motivoconsulta;
    public String queixaprincipal;
    public String descricaosintomas;
    public String iniciosintomas;
    public String alergias;
    public String medicacao;
    public String datatriagem;

    // USERPROFILE
    public UserProfile userprofile;

    public static class UserProfile {
        public int id;
        public String nome;
        public String email;
        public String sns;
    }

    // PULSEIRA
    public Pulseira pulseira;

    public static class Pulseira {
        public int id;
        public String codigo;
        public String prioridade;
        public String status;
        public String tempoentrada;

        // GETTERS NECESSÁRIOS PARA A PULSEIRA
        public int getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getPrioridade() { return prioridade; }
        public String getStatus() { return status; }
        public String getTempoentrada() { return tempoentrada; }
    }

    // CONSULTA ASSOCIADA
    public Consulta consulta;

    public static class Consulta {
        public int id;
        public String estado;
    }

    //  GETTERS PRINCIPAIS DA TRIAGEM

    public int getId() {
        return id;
    }

    public String getDataTriagem() {
        return datatriagem;
    }

    public String getMotivo() {
        return motivoconsulta;
    }

    public String getQueixa() {
        return queixaprincipal;
    }

    //  MÉTODOS AUXILIARES

    public String getNomePaciente() {
        if (userprofile != null && userprofile.nome != null) {
            return userprofile.nome;
        }
        return "Anónimo";
    }

    public String getSnsPaciente() {
        if (userprofile != null && userprofile.sns != null) {
            return userprofile.sns;
        }
        return "---";
    }
}