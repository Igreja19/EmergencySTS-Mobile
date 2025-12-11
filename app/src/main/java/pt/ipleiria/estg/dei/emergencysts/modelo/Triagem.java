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
    }

    // CONSULTA ASSOCIADA
    public Consulta consulta;

    public static class Consulta {
        public int id;
        public String estado;
    }
}
