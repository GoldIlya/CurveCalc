package ru.itinbiz.curvecalc;



import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidplot.xy.SimpleXYSeries;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.itinbiz.curvecalc.adapter.ZamerAdapter;
import ru.itinbiz.curvecalc.data.AppDatabase;
import ru.itinbiz.curvecalc.model.Measurement;

public class AddZamerActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOAD_JSON = 101;
    private static final int REQUEST_PERMISSIONS = 102;
    private FloatingActionButton btnAddZamer;
    private RecyclerView recyclerView;

    String zamerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_zamer);
        recyclerView = findViewById(R.id.listZamer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getZamerList();
        btnAddZamer = findViewById(R.id.btnAddZamer);

        btnAddZamer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inflate the custom layout
                View view = LayoutInflater.from(AddZamerActivity.this).inflate(R.layout.dialog_input_with_radio_buttons, null);

            // Get the EditText and RadioGroup from the custom layout
                EditText input = view.findViewById(R.id.input);
                RadioGroup radioGroup = view.findViewById(R.id.radioGroup);

                // Set the default selection for the RadioGroup
                radioGroup.check(R.id.radioBtnPoint);

                // Create the AlertDialog with the custom layout
                AlertDialog dialogWithInput = new AlertDialog.Builder(AddZamerActivity.this)
                        .setTitle("Добавить замер")
                        .setMessage("Введите название замера:")
                        .setView(view)
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                zamerName = String.valueOf(input.getText());
                                Boolean loadfile;
                                int selectedId = radioGroup.getCheckedRadioButtonId();
                                RadioButton selectedRadioButton = view.findViewById(selectedId);
                                String measurementUnit = selectedRadioButton.getText().toString();
                                if(measurementUnit.equals("Загрузить из файла")){
                                    loadfile = true;
                                    Intent intent = new Intent(AddZamerActivity.this, ZamerActivity.class);
                                    intent.putExtra("zamerName", zamerName)
                                            .putExtra("loadfile", loadfile)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }else{
                                    loadfile = false;
                                    Intent intent = new Intent(AddZamerActivity.this, ZamerActivity.class);
                                    intent.putExtra("zamerName", zamerName)
                                            .putExtra("measurementUnit", measurementUnit)
                                            .putExtra("loadfile", loadfile)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }
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
                ZamerAdapter adapter = new ZamerAdapter(AddZamerActivity.this, measurementList);
                recyclerView.setAdapter(adapter);
            }
        }

        GetZamerList gZamer = new GetZamerList();
        gZamer.execute();
    }



    public void onBackPressed() {
        Intent intent = new Intent(AddZamerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


}