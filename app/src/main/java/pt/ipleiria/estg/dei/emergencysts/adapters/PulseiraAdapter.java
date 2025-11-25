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
import pt.ipleiria.estg.dei.emergencysts.modelo.Pulseira;

public class PulseiraAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Pulseira> pulseiras;
    private LayoutInflater inflater;

    public PulseiraAdapter(Context context, ArrayList<Pulseira> pulseiras) {
        this.context = context;
        this.pulseiras = pulseiras;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return pulseiras.size();
    }

    @Override
    public Object getItem(int position) {
        return pulseiras.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_pulseira, parent, false);
            holder = new ViewHolder();
            holder.textViewNomePaciente = convertView.findViewById(R.id.textViewNomePaciente);
            holder.textViewPrioridade = convertView.findViewById(R.id.textViewPrioridade);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Pulseira pulseira = pulseiras.get(position);
        holder.textViewNomePaciente.setText(pulseira.getNomePaciente());
        holder.textViewPrioridade.setText(pulseira.getPrioridade());

        // Define a cor do texto com base na prioridade
        switch (pulseira.getPrioridade().toLowerCase()) {
            case "vermelho":
                holder.textViewPrioridade.setTextColor(Color.RED);
                break;
            case "laranja":
                holder.textViewPrioridade.setTextColor(Color.rgb(255, 165, 0));
                break;
            case "amarelo":
                holder.textViewPrioridade.setTextColor(Color.YELLOW);
                break;
            case "verde":
                holder.textViewPrioridade.setTextColor(Color.GREEN);
                break;
            case "azul":
                holder.textViewPrioridade.setTextColor(Color.BLUE);
                break;
            default:
                holder.textViewPrioridade.setTextColor(Color.BLACK);
                break;
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView textViewNomePaciente;
        TextView textViewPrioridade;
    }
}
