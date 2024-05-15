package ru.itinbiz.curvecalc.adapter;

        import android.content.Context;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.TextView;

        import androidx.recyclerview.widget.RecyclerView;

        import com.androidplot.xy.SimpleXYSeries;

        import ru.itinbiz.curvecalc.R;


public class DiffAdapter extends RecyclerView.Adapter<DiffAdapter.PointViewHolder> {

    private Context mCtx;
    private SimpleXYSeries dataSet;

    public interface OnItemClickListener {
        void onItemClick(String number, String value, int index);
    }


    public DiffAdapter(Context mCtx, SimpleXYSeries dataSet) {
        this.mCtx = mCtx;
        this.dataSet = dataSet;

    }

    @Override
    public PointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mCtx).inflate(R.layout.text_row_item, parent, false);
        return new PointViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PointViewHolder holder, int position) {
        int result = Math.round(dataSet.getX(position).floatValue());
        String pointX = String.valueOf(result);
        holder.tvPoint.setText(" "+pointX);

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
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    String number = String.valueOf(Math.round(dataSet.getY(getAdapterPosition()).floatValue()));
//                    String value = String.valueOf(Math.round(dataSet.getX(getAdapterPosition()).floatValue()));
//                    int index = getAdapterPosition();
//                }
//            });



        }
    }
}

