package pt.ipleiria.estg.dei.emergencysts.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
            holder.tvNome = convertView.findViewById(R.id.tvNome);
            holder.tvSNS = convertView.findViewById(R.id.tvSNS);
            holder.tvHora = convertView.findViewById(R.id.tvHora);
            holder.tvStatus = convertView.findViewById(R.id.tvStatus);
            holder.imgDot = convertView.findViewById(R.id.imgDot);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Pulseira pulseira = pulseiras.get(position);

        holder.tvNome.setText(pulseira.getNomePaciente());

        holder.tvSNS.setText("SNS: " + (pulseira.getSns() != null ? pulseira.getSns() : "---"));

        holder.tvHora.setText(pulseira.getHora() != null ? pulseira.getHora() : "--:--");

        holder.tvStatus.setText(pulseira.getPrioridade());
        holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_pendente);
        holder.imgDot.setColorFilter(Color.parseColor("#9E9E9E"));


        return convertView;
    }

    private static class ViewHolder {
        TextView tvNome;
        TextView tvSNS;
        TextView tvHora;
        TextView tvStatus;
        ImageView imgDot;
    }
}
