package pt.ipleiria.estg.dei.emergencysts.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import pt.ipleiria.estg.dei.emergencysts.R;
import pt.ipleiria.estg.dei.emergencysts.modelo.Triagem;
import pt.ipleiria.estg.dei.emergencysts.listeners.TriagemListener;
import pt.ipleiria.estg.dei.emergencysts.utils.SharedPrefManager;

public class TriagemAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Triagem> triagens;
    private final LayoutInflater inflater;
    private final TriagemListener listener;

    public TriagemAdapter(Context context, ArrayList<Triagem> triagens, TriagemListener listener) {
        this.context = context;
        this.triagens = triagens;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return triagens.size();
    }

    @Override
    public Object getItem(int position) {
        return triagens.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_triagem, parent, false);

            holder = new ViewHolder();
            holder.tvNome = convertView.findViewById(R.id.tvNome);
            holder.tvSNS = convertView.findViewById(R.id.tvSNS);
            holder.tvStatus = convertView.findViewById(R.id.tvStatus);
            holder.tvData = convertView.findViewById(R.id.tvData);
            holder.tvHora = convertView.findViewById(R.id.tvHora);
            holder.tvQueixa = convertView.findViewById(R.id.tvQueixa);
            holder.tvEnfermeiro = convertView.findViewById(R.id.tvEnfermeiro);
            holder.dotPrioridade = convertView.findViewById(R.id.dotPrioridade);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Triagem t = triagens.get(position);

        // ---------------------------
        // USERPROFILE → Nome + SNS
        // ---------------------------
        if (t.userprofile != null) {
            holder.tvNome.setText(t.userprofile.nome);
            holder.tvSNS.setText("SNS: " + t.userprofile.sns);
        }

        // ---------------------------
        // DATA + HORA formatadas (yyyy-mm-dd HH:mm:ss → dd/mm/yyyy + HH:mm)
        // ---------------------------
        if (t.datatriagem != null && t.datatriagem.contains(" ")) {
            String[] partes = t.datatriagem.split(" ");
            if (partes.length == 2) {
                String[] data = partes[0].split("-");
                if (data.length == 3) {
                    holder.tvData.setText(data[2] + "/" + data[1] + "/" + data[0]);
                }
                holder.tvHora.setText(partes[1].substring(0, 5)); // HH:mm
            }
        }

        // ---------------------------
        // QUEIXA
        // ---------------------------
        holder.tvQueixa.setText("Queixa: " + t.queixaprincipal);

        // ---------------------------
        // STATUS (fixo: CONCLUÍDA)
        // ---------------------------
        holder.tvStatus.setText("Concluída");
        holder.tvStatus.setTextColor(Color.parseColor("#1DB954"));

        // ---------------------------
        // ENFERMEIRO = utilizador atual
        // ---------------------------
        String enfermeiroAtual = SharedPrefManager.getInstance(context).getEnfermeiro().getUsername();
        holder.tvEnfermeiro.setText("Enf. " + enfermeiroAtual);

        // ---------------------------
        // PRIORIDADE (bolinha com cor)
        // ---------------------------
        if (t.pulseira != null && t.pulseira.prioridade != null) {

            String prioridade = t.pulseira.prioridade.toLowerCase();

            switch (prioridade) {
                case "vermelho":
                    holder.dotPrioridade.setBackgroundResource(R.drawable.circle_red);
                    break;
                case "laranja":
                    holder.dotPrioridade.setBackgroundResource(R.drawable.circle_orange);
                    break;
                case "amarela":
                    holder.dotPrioridade.setBackgroundResource(R.drawable.circle_yellow);
                    break;
                case "verde":
                    holder.dotPrioridade.setBackgroundResource(R.drawable.circle_green);
                    break;
                case "azul":
                    holder.dotPrioridade.setBackgroundResource(R.drawable.circle_blue);
                    break;
                default:
                    holder.dotPrioridade.setBackgroundResource(R.drawable.circle_gray);
            }
        } else {
            holder.dotPrioridade.setBackgroundResource(R.drawable.circle_gray);
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    // Envia o ID da triagem para quem estiver a ouvir (a Activity)
                    listener.onTriagemClick(t.id);
                }
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView tvNome, tvSNS, tvStatus, tvData, tvHora, tvQueixa, tvEnfermeiro;
        View dotPrioridade;
    }
}
