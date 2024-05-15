package ru.itinbiz.curvecalc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.androidplot.xy.SimpleXYSeries;

import ru.itinbiz.curvecalc.R;


public class PointAdapter extends RecyclerView.Adapter<PointAdapter.PointViewHolder> {

    private Context mCtx;
    private SimpleXYSeries dataSet, diffSet;
    private OnItemClickListener onItemClickListener;


    public interface OnItemClickListener {
        void onItemClick(String number, String value, int index);
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
        String diff;
        if(diffSet.size()>0){
            diff = String.valueOf(diffSet.getX(position).floatValue());
        }else{
            diff = "";
        }
        holder.tvPoint.setText(pointY+" "+pointX);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    class PointViewHolder extends RecyclerView.ViewHolder {

        TextView tvPoint;
        public PointViewHolder(View itemView) {

            super(itemView);

            tvPoint = itemView.findViewById(R.id.textView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String number = String.valueOf(Math.round(dataSet.getY(getAdapterPosition()).floatValue()));
                    String value = String.valueOf(Math.round(dataSet.getX(getAdapterPosition()).floatValue()));
                    int index = getAdapterPosition();
                    onItemClickListener.onItemClick(number, value, index);
                }
            });
        }
    }

}

