package ru.itinbiz.curvecalc;



import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
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
import com.google.gson.JsonSyntaxException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        checkPermissions();
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
                                    // Открываем файловый диалог для выбора JSON-файла
                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                                    intent.setType("application/json");
                                    startActivityForResult(intent, REQUEST_CODE_LOAD_JSON);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LOAD_JSON && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Measurement measurementLF = loadDataFromJsonFile(uri);

                // Передаем объект Measurement в ZamerActivity
                Intent intent = new Intent(AddZamerActivity.this, ZamerActivity.class);
                Gson gson = new Gson();
                String seriesListJson = measurementLF.getSeriesListJson();
                String curElementsToJson = measurementLF.getCurElementsJson();
                String pointShiftJson = measurementLF.getPointShiftJson();
                String nameZamer = zamerName;
                String measurementUnit = measurementLF.getMeasurementUnit();
                Double countPointLF = measurementLF.getCountPoint();
                int countSeriesLF = measurementLF.getCountSeries();
                boolean loadfile = true;

                intent.putExtra("seriesListJson", seriesListJson)
                        .putExtra("nameZamer", nameZamer)
                        .putExtra("curElementsToJson", curElementsToJson)
                        .putExtra("pointShiftJson", pointShiftJson)
                        .putExtra("measurementUnit", measurementUnit)
                        .putExtra("countPointLF", countPointLF)
                        .putExtra("countSeriesLF", countSeriesLF)
                        .putExtra("loadfile", loadfile);
                startActivity(intent);
                finish();
            }
        }


    }

    private Measurement loadDataFromJsonFile(Uri fileUri) {
        Measurement measurement = new Measurement();
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             InputStreamReader reader = new InputStreamReader(inputStream)) {

            // Читаем данные из файла
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            String jsonString = new String(bytes);

            // Логируем содержимое JSON для отладки
            Log.d("JSON_CONTENT", jsonString);

            // Парсим основной JSON-объект
            Gson gson = new Gson();
            Type mainType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> dataMap = gson.fromJson(jsonString, mainType);

            // Извлекаем и парсим seriesListJson
            if (dataMap.containsKey("seriesListJson")) {
                String seriesListJsonString = (String) dataMap.get("seriesListJson");

                // Убираем экранирование и парсим как JSON-массив
                Type seriesListType = new TypeToken<ArrayList<SimpleXYSeries>>() {}.getType();
                ArrayList<SimpleXYSeries> seriesList = gson.fromJson(seriesListJsonString, seriesListType);

                // Конвертируем обратно в JSON-строку для хранения
                measurement.setSeriesListJson(gson.toJson(seriesList));
            }

            // Извлекаем и парсим pointShiftJson
            if (dataMap.containsKey("pointShiftJson")) {
                String pointShiftJsonString = (String) dataMap.get("pointShiftJson");

                // Убираем экранирование и парсим как JSON-объект
                Type pointShiftType = new TypeToken<Map<Integer, Integer>>() {}.getType();
                Map<Integer, Integer> pointShiftMap = gson.fromJson(pointShiftJsonString, pointShiftType);

                // Конвертируем обратно в JSON-строку для хранения
                measurement.setPointShiftJson(gson.toJson(pointShiftMap));
            }

            // Извлекаем и парсим curElementsToJson
            if (dataMap.containsKey("curElementsToJson")) {
                String curElementsJsonString = (String) dataMap.get("curElementsToJson");

                // Убираем экранирование и парсим как JSON-объект
                Type curElementsType = new TypeToken<Map<Integer, Integer>>() {}.getType();
                Map<Integer, Integer> curElementsMap = gson.fromJson(curElementsJsonString, curElementsType);

                // Конвертируем обратно в JSON-строку для хранения
                measurement.setCurElementsJson(gson.toJson(curElementsMap));
            }

            // Извлекаем measurementUnit
            if (dataMap.containsKey("measurementUnit")) {
                measurement.setMeasurementUnit((String) dataMap.get("measurementUnit"));
            }

            // Извлекаем countPointLF
            if (dataMap.containsKey("countPointLF")) {
                measurement.setCountPoint(Double.parseDouble(dataMap.get("countPointLF").toString()));
            }

            // Извлекаем countSeriesLF
            if (dataMap.containsKey("countSeriesLF")) {
                measurement.setCountSeries(Integer.parseInt(dataMap.get("countSeriesLF").toString()));
            }

            // Устанавливаем имя замера
            measurement.setName(zamerName); // Используем имя замера, введенное пользователем

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при загрузке файла", Toast.LENGTH_SHORT).show();
            return null; // или выбрасывайте исключение
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при парсинге JSON", Toast.LENGTH_SHORT).show();
            return null; // или выбрасывайте исключение
        }

        return measurement;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено
            } else {
                Toast.makeText(this, "Разрешение на чтение файлов не предоставлено", Toast.LENGTH_SHORT).show();
            }
        }
    }

}