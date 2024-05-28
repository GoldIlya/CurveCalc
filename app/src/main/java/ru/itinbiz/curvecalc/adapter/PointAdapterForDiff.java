package ru.itinbiz.curvecalc.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.androidplot.xy.SimpleXYSeries;

import java.util.List;

import ru.itinbiz.curvecalc.R;


public class PointAdapterForDiff extends RecyclerView.Adapter<PointAdapterForDiff.PointViewHolder> {

    private Context mCtx;
    private SimpleXYSeries dataSet, diffSet;
    private List<Boolean> listChange;
    private OnItemClickListener onItemClickListener;
    private int selectedIndex = -1; // Variable to store selected index


    public interface OnItemClickListener {
        void onItemClick(String number, String value, int index);
    }


    public PointAdapterForDiff(Context mCtx, SimpleXYSeries dataSet, SimpleXYSeries diffSet, List<Boolean> curListChange, OnItemClickListener onItemClickListener) {
        this.mCtx = mCtx;
        this.dataSet = dataSet;
        this.diffSet = diffSet;
        this.onItemClickListener = onItemClickListener;
        this.listChange = curListChange;
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
        boolean isInteger = (dataSet.getY(position).floatValue() - Math.floor(dataSet.getY(position).floatValue())) == 0;

        if(diffSet.size()>0){
            diff = String.valueOf(diffSet.getX(position).floatValue());
        }else{
            diff = "";
        }
        if (isInteger){
            holder.tvNumber.setText(pointY+" ");
            holder.tvZnach.setText(pointX+" ");
            holder.tvDiff.setText(diff);
        }else{
            holder.tvNumber.setText("- ");
            holder.tvZnach.setText(pointX+" ");
            holder.tvDiff.setText(diff);
        }

        if(!diff.equals("0.0") && listChange.get(position)){
            if(isInteger){
                holder.tvNumber.setTextColor(mCtx.getResources().getColor(R.color.green));
                holder.tvZnach.setTextColor(mCtx.getResources().getColor(R.color.green));
                holder.tvDiff.setTextColor(mCtx.getResources().getColor(R.color.green));
            }else {
                holder.tvNumber.setTextColor(mCtx.getResources().getColor(R.color.blue));
                holder.tvZnach.setTextColor(mCtx.getResources().getColor(R.color.blue));
                holder.tvDiff.setTextColor(mCtx.getResources().getColor(R.color.blue));
            }
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
                    String number = String.valueOf(dataSet.getY(getAdapterPosition()).floatValue());
                    String value = String.valueOf(dataSet.getX(getAdapterPosition()).floatValue());
                    int index = getAdapterPosition();

                    selectedIndex = index;
                    notifyDataSetChanged();

                    onItemClickListener.onItemClick(number, value, index);
                }
            });
        }
    }

}

