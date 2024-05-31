package ru.itinbiz.curvecalc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.androidplot.xy.SimpleXYSeries;

import ru.itinbiz.curvecalc.R;


public class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {

    private Context mCtx;
    private SimpleXYSeries dataSet, diffSet;
    private OnItemClickListener onItemClickListener;
    private int selectedIndex = -1;


    public interface OnItemClickListener {
        void onItemClick(String number, Double numberDouble, String value, int index);
    }


    public PointAdapter(Context mCtx, SimpleXYSeries dataSet, SimpleXYSeries diffSet, OnItemClickListener onItemClickListener) {
        this.mCtx = mCtx;
        this.dataSet = dataSet;
        this.diffSet = diffSet;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public PointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mCtx).inflate(R.layout.text_row_item, parent, false);
        return new PointViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PointViewHolder holder, int position) {
        int resultY = Math.round(dataSet.getY(position).floatValue());
        String pointX =  String.valueOf(dataSet.getX(position).floatValue());
        String pointY = String.valueOf(resultY);
        boolean isInteger = (dataSet.getY(position).floatValue() - Math.floor(dataSet.getY(position).floatValue())) == 0;
        if(isInteger){
            holder.tvNumber.setText(pointY);
            holder.tvZnach.setText(" "+pointX);
        }
        if(position >= 1 && position < dataSet.size()-1){
            String pointYprev = String.valueOf(Math.round(dataSet.getY(position-1).floatValue()));
            String pointYforvard = String.valueOf(Math.round(dataSet.getY(position+1).floatValue()));
            if(!isInteger){
                holder.tvNumber.setText(pointYprev+"/"+pointYforvard);
                holder.tvZnach.setText(pointX);
            }
        }

        String diff;
        if(diffSet.size()>0){
            diff = String.valueOf(diffSet.getX(position).floatValue());
        }else{
            diff = "";
        }

        // Highlight the selected item
        if (position == selectedIndex) {
            holder.itemView.setBackgroundColor(mCtx.getResources().getColor(R.color.selected_item_background)); // Set your highlight color
        } else {
            holder.itemView.setBackgroundColor(mCtx.getResources().getColor(android.R.color.transparent)); // Reset to default color
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    class PointViewHolder extends RecyclerView.ViewHolder {

        TextView tvNumber, tvZnach, tvDiff;
        public PointViewHolder(View itemView) {

            super(itemView);

            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvZnach = itemView.findViewById(R.id.tvZnach);
            tvDiff = itemView.findViewById(R.id.tvDiff);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Double numberDouble = dataSet.getY(getAdapterPosition()).doubleValue();
                    Toast.makeText(mCtx, "Позиция"+getAdapterPosition(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(mCtx, "Размер списка"+ dataSet.size(), Toast.LENGTH_SHORT).show();
                    String number, pointYprev, pointYforvard;
                    pointYprev = "null";
                    pointYforvard = "null";



                    if(getAdapterPosition()>=1 && (dataSet.size()-1)-getAdapterPosition() >= 1){
                        pointYforvard = (dataSet.getY(getAdapterPosition()+1).toString());
                        pointYprev = (dataSet.getY(getAdapterPosition()-1).toString());
                    }else{if(getAdapterPosition()<1 && (dataSet.size()-1)-getAdapterPosition() >= 1){
                        pointYforvard = (dataSet.getY(getAdapterPosition()+1).toString());
                        pointYprev = "*";
                    }
                        if(getAdapterPosition()>=1 && (dataSet.size()-1)-getAdapterPosition() < 1){
                            pointYforvard = "*";
                            pointYprev = (dataSet.getY(getAdapterPosition()-1).toString());;
                        }
                    }
                    int resultY = Math.round(dataSet.getY(getAdapterPosition()).floatValue());
                    String pointY = String.valueOf(resultY);
                    boolean isInteger = (dataSet.getY(getAdapterPosition()).floatValue() - Math.floor(dataSet.getY(getAdapterPosition()).floatValue())) == 0;
                    if(isInteger){
                        number = pointY;
                    }else{
                        number= pointYprev+"/"+pointYforvard;
                    }
                    String value = String.valueOf(dataSet.getX(getAdapterPosition()).floatValue());
                    int index = getAdapterPosition();
                    selectedIndex = index;
                    notifyDataSetChanged();
                    onItemClickListener.onItemClick(number, numberDouble, value, index);
                }
            });
        }
    }

}