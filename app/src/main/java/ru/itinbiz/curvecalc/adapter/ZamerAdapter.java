package ru.itinbiz.curvecalc.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import ru.itinbiz.curvecalc.MainActivity;
import ru.itinbiz.curvecalc.R;
import ru.itinbiz.curvecalc.ZamerActivity;
import ru.itinbiz.curvecalc.data.AppDatabase;
import ru.itinbiz.curvecalc.model.Measurement;

public class ZamerAdapter extends RecyclerView.Adapter<ZamerAdapter.ZamerViewHolder> {

    private Context mCtx;
    private List<Measurement> measurementList;

    public ZamerAdapter(Context mCtx, List<Measurement> measurementList) {
        this.mCtx = mCtx;
        this.measurementList = measurementList;
    }

    @Override
    public ZamerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mCtx).inflate(R.layout.recyclerview_zamer, parent, false);
        return new ZamerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ZamerViewHolder holder, int position) {
        Measurement oMeasurement = measurementList.get(position);
        holder.tvNameZamer.setText(oMeasurement.getName());
    }

    @Override
    public int getItemCount() {
        return measurementList.size();
    }

    class ZamerViewHolder extends RecyclerView.ViewHolder {

        TextView tvNameZamer;
        FloatingActionButton btnDel;

        public ZamerViewHolder(View itemView) {

            super(itemView);

            tvNameZamer = itemView.findViewById(R.id.nameLabel);
            btnDel = itemView.findViewById(R.id.btnDel);

            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Measurement oMeasurement = measurementList.get(getAdapterPosition());
                    int measurementId = oMeasurement.getId();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
                    builder.setTitle("Вы действительно хотите удалить?");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteMeasurement(measurementId);
                            Intent intent = new Intent(mCtx, MainActivity.class);
                            mCtx.startActivity(intent);
                        }
                    });
                    builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    AlertDialog ad = builder.create();
                    ad.show();
                }
            });


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Measurement oMeasurement = measurementList.get(getAdapterPosition());
                    int measurementId = oMeasurement.getId();
                    Intent intent = new Intent(mCtx, ZamerActivity.class);
                    intent.putExtra("measurementId", measurementId).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mCtx.startActivity(intent);
                }
            });
        }

    }

    private void deleteMeasurement (int id) {

        class DeleteMeasurement extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {

                AppDatabase.getDatabase(mCtx)
                        .measurementDao()
                        .deleteMeasurementById(id);
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }
        DeleteMeasurement dM = new DeleteMeasurement();
        dM.execute();
    }

}

