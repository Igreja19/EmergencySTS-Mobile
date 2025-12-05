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

    // OBJETO USERPROFILE
    public UserProfile userprofile;

    public static class UserProfile {
        public int id;
        public String nome;
        public String email;
        public String sns;
    }

    // OBJETO PULSEIRA ASSOCIADA
    public Pulseira pulseira;

    public static class Pulseira {
        public int id;
        public String codigo;
        public String prioridade;
        public String status;
        public String tempoentrada;
    }
}
