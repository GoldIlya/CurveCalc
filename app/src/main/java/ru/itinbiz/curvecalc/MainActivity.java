package ru.itinbiz.curvecalc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import ru.itinbiz.curvecalc.adapter.ZamerAdapter;
import ru.itinbiz.curvecalc.data.AppDatabase;
import ru.itinbiz.curvecalc.model.Measurement;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton btnAddZamer;
    private RecyclerView recyclerView;

    String zamerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.listZamer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getZamerList();
        btnAddZamer = findViewById(R.id.btnAddZamer);

        btnAddZamer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the custom layout
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_input_with_radio_buttons, null);

            // Get the EditText and RadioGroup from the custom layout
                EditText input = view.findViewById(R.id.input);
                RadioGroup radioGroup = view.findViewById(R.id.radioGroup);

                // Set the default selection for the RadioGroup
                radioGroup.check(R.id.radioButton10m);

                // Create the AlertDialog with the custom layout
                AlertDialog dialogWithInput = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Добавить замер")
                        .setMessage("Введите название замера:")
                        .setView(view)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                zamerName = String.valueOf(input.getText());
                                int selectedId = radioGroup.getCheckedRadioButtonId();
                                RadioButton selectedRadioButton = view.findViewById(selectedId);
                                String measurementUnit = selectedRadioButton.getText().toString();
                                Intent intent = new Intent(MainActivity.this, ZamerActivity.class);
                                intent.putExtra("zamerName", zamerName)
                                        .putExtra("measurementUnit", measurementUnit)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();

                dialogWithInput.show();
            }
        });

    }



    private void getZamerList() {
        class GetZamerList extends AsyncTask<Void, Void, List<Measurement>> {

            @Override
            protected List<Measurement> doInBackground(Void... voids) {
                List<Measurement> measurementList = AppDatabase
                        .getDatabase(getApplicationContext())
                        .measurementDao()
                        .getAllMeasurements();
                return measurementList;
            }

            @Override
            protected void onPostExecute( List<Measurement> measurementList) {
                super.onPostExecute(measurementList);
                ZamerAdapter adapter = new ZamerAdapter(MainActivity.this, measurementList);
                recyclerView.setAdapter(adapter);
            }
        }

        GetZamerList gZamer = new GetZamerList();
        gZamer.execute();
    }

}