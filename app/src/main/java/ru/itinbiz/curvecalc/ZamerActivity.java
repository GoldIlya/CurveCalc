package ru.itinbiz.curvecalc;

import static android.view.KeyEvent.KEYCODE_ENTER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ru.itinbiz.curvecalc.adapter.PointAdapter;
import ru.itinbiz.curvecalc.adapter.PointAdapterForDiff;
import ru.itinbiz.curvecalc.data.AppDatabase;
import ru.itinbiz.curvecalc.data.MeasurementDao;
import ru.itinbiz.curvecalc.model.Measurement;

public class ZamerActivity extends AppCompatActivity implements PointAdapter.OnItemClickListener, PointAdapterForDiff.OnItemClickListener {

    int measurementIdDb;
    boolean isNew = false;
    private XYPlot plot;
    private SimpleXYSeries series, curSeries,  highlightedPoint;
    private RecyclerView recyclerView;
    private LineAndPointFormatter seriesFormat, seriesFormatPromer, pointFormat;
    private List<SimpleXYSeries> seriesList = new ArrayList<>();
    private int curElement;
    private int count = 0;
    private int countPoint = 1;
    private int countSeries = 0;

    private LinearLayout blockSdvig, blockSeries, blockEdit, blockAddPoint;
    private Button addButton, btnPlus, btnMinus, createNewSeriesButton, prevButton, nextButton, appEdit, clearListPoint, resetChanges;
    private EditText xCoordinateEditText, valueSet;
    private TextView countTextView;
    private Spinner seriesSpinner;
    private ArrayAdapter<String> seriesAdapter;
    private int selectedSeriesIndex;
    private String zamerNameDB;
    private AppDatabase appDatabase;
    private MeasurementDao measurementDao;
    private LiveData<Measurement> lastMeasurement;
    private Measurement measurementDB;

    private String measurementUnitDB;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zamer);


        // Initialize the database
        appDatabase = AppDatabase.getDatabase(this);
        measurementDao = appDatabase.measurementDao();




        plot = findViewById(R.id.plot);
        seriesSpinner = findViewById(R.id.series_spinner);
        addButton = findViewById(R.id.addButton);
        xCoordinateEditText = findViewById(R.id.xCoordinateEditText);
        valueSet = findViewById(R.id.valueSet);
        appEdit = findViewById(R.id.appEdit);
        btnPlus = findViewById(R.id.increment_button);
        btnMinus = findViewById(R.id.decrement_button);
        prevButton = findViewById(R.id.prev_button);
        nextButton = findViewById(R.id.next_button);
        createNewSeriesButton = findViewById(R.id.create_new_series_button);
        clearListPoint = findViewById(R.id.clearListPoint);
        resetChanges = findViewById(R.id.resetChange);
        blockSdvig = findViewById(R.id.blockSdvig);
        blockSeries = findViewById(R.id.blockSeries);
        blockEdit = findViewById(R.id.blockEdit);
        blockAddPoint = findViewById(R.id.blockAddPoint);
        if(seriesList.size()==0 || seriesSpinner.getSelectedItemPosition()==0 ){
            blockSdvig.setVisibility(View.GONE);
            blockEdit.setVisibility(View.GONE);
            blockAddPoint.setVisibility(View.VISIBLE);
            clearListPoint.setVisibility(View.VISIBLE);
            resetChanges.setVisibility(View.GONE);
        }else{
            blockEdit.setVisibility(View.GONE);
            blockAddPoint.setVisibility(View.GONE);
            clearListPoint.setVisibility(View.GONE);
            resetChanges.setVisibility(View.VISIBLE);
        }

        recyclerView = findViewById(R.id.listPoint);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));

        if(getIntent().getSerializableExtra("measurementId")!= null){
            final int measurementId = (int) getIntent().getSerializableExtra("measurementId");

           isNew = false;
            measurementIdDb = measurementId;
            measurementDB = getMeasurement(measurementId);
            zamerNameDB = measurementDB.getName();
//            Toast.makeText(this, "Имя "+ zamerNameDB, Toast.LENGTH_SHORT).show();
            Gson gson = new Gson();
            Type seriesListType = new TypeToken<ArrayList<SimpleXYSeries>>() {}.getType();
            seriesList = gson.fromJson(measurementDB.getSeriesListJson(), seriesListType);
            if(seriesList.size()>0){
                countPoint = measurementDB.getCountPoint();
                countSeries = measurementDB.getCountSeries();
                measurementUnitDB = measurementDB.getMeasurementUnit();
            }else{
                series = new SimpleXYSeries("Замер");
                seriesList.add(series); // Add the initial series to the list
            }

            String[] newSeriesArray = getSeriesArray();
            ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, newSeriesArray);
            newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            seriesSpinner.setAdapter(newSeriesAdapter);
            seriesSpinner.setSelection(0);
            Toast.makeText(this, "Хорда "+ measurementUnitDB, Toast.LENGTH_SHORT).show();

        }else{
            isNew = true;
            final String zamerName = (String) getIntent().getSerializableExtra("zamerName");
            final String measurementUnit = (String) getIntent().getSerializableExtra("measurementUnit");
            measurementUnitDB = measurementUnit;
            zamerNameDB = zamerName;
            Toast.makeText(this, "Хорда "+ measurementUnitDB, Toast.LENGTH_SHORT).show();
            series = new SimpleXYSeries("Замер");
            seriesList.add(series); // Add the initial series to the list
        }

        countTextView = findViewById(R.id.count_text_view);

        curSeries = seriesList.get(0);

        // Создаём разные форматы для отображения на графике
        seriesFormat = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        seriesFormat.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeries, int index) {
                return "";

            }
        });

        seriesFormatPromer = new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        seriesFormatPromer.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeries, int index) {
                if(calculateDifference().getX(index).floatValue() == 0.0){
                    return "";
                }else{
                    return ""+calculateDifference().getX(index).floatValue();
                }
            }
        });


        pointFormat = new LineAndPointFormatter(this, R.xml.formatter_point);
        pointFormat.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeries, int index) {
                return ""+calculateDifference().getX(curElement).floatValue();
//                return "";
            }
        });


        // Add the series to the plot with the formatter
        if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
            plot.addSeries(curSeries, seriesFormat);
        }else{
            plot.addSeries(curSeries, seriesFormatPromer);
        }

        // Set the plot's properties
        plot.setRangeLabel("Y");
        plot.setDomainLabel("X");
        plot.setDomainStep(StepMode.INCREMENT_BY_PIXELS,30);
        plot.setRangeStep(StepMode.INCREMENT_BY_PIXELS,50);
        PanZoom.attach(plot, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.SCALE);
        calculateDifference();
        createListPoint();
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String xCoordinateString = xCoordinateEditText.getText().toString();
                String yCoordinateString = String.valueOf(countPoint);

                if(seriesList.size()>1){
                    AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                    builder.setTitle("Данное действие удалит все шаги.");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (!xCoordinateString.isEmpty() &&!yCoordinateString.isEmpty()) {
                                float xCoordinate = Float.parseFloat(xCoordinateString);
                                float yCoordinate = Float.parseFloat(yCoordinateString);
                                addDataPoint(xCoordinate, yCoordinate);
                                // Delete all elements in seriesList except the current one
                                int currentIndex = seriesSpinner.getSelectedItemPosition();
                                seriesList.clear();
                                seriesList.add(curSeries);
                                seriesSpinner.setSelection(currentIndex);
                                createListPoint();
                                setScalePlot();
                                resetCount();
                                countSeries = 0;
                            }
                        }
                    });
                    builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    AlertDialog ad = builder.create();
                    ad.show();
                }else{
                    if (!xCoordinateString.isEmpty() &&!yCoordinateString.isEmpty()) {
                        float xCoordinate = Float.parseFloat(xCoordinateString);
                        float yCoordinate = Float.parseFloat(yCoordinateString);
                        addDataPoint(xCoordinate, yCoordinate);
                        // Delete all elements in seriesList except the current one
                        int currentIndex = seriesSpinner.getSelectedItemPosition();
                        seriesList.clear();
                        seriesList.add(curSeries);
                        seriesSpinner.setSelection(currentIndex);
                        createListPoint();
                        setScalePlot();
                        resetCount();
                        countSeries = 0;
//                        Toast.makeText(MainActivity.this, "Счётчик"+countSeries, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });



        xCoordinateEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean consumed = false;
                if (keyCode == KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        String xCoordinateString = xCoordinateEditText.getText().toString();
                        String yCoordinateString = String.valueOf(countPoint);

                        if(seriesList.size()>1){
                            AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                            builder.setTitle("Данное действие удалит все шаги.");
                            builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (!xCoordinateString.isEmpty() &&!yCoordinateString.isEmpty()) {
                                        float xCoordinate = Float.parseFloat(xCoordinateString);
                                        float yCoordinate = Float.parseFloat(yCoordinateString);
                                        addDataPoint(xCoordinate, yCoordinate);
                                        // Delete all elements in seriesList except the current one
                                        int currentIndex = seriesSpinner.getSelectedItemPosition();
                                        seriesList.clear();
                                        seriesList.add(curSeries);
                                        seriesSpinner.setSelection(currentIndex);
                                        createListPoint();
                                        setScalePlot();
                                        resetCount();
                                        countSeries = 0;
                                    }
                                }
                            });
                            builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            AlertDialog ad = builder.create();
                            ad.show();
                        }else{
                            if (!xCoordinateString.isEmpty() &&!yCoordinateString.isEmpty()) {
                                float xCoordinate = Float.parseFloat(xCoordinateString);
                                float yCoordinate = Float.parseFloat(yCoordinateString);
                                addDataPoint(xCoordinate, yCoordinate);
                                // Delete all elements in seriesList except the current one
                                int currentIndex = seriesSpinner.getSelectedItemPosition();
                                seriesList.clear();
                                seriesList.add(curSeries);
                                seriesSpinner.setSelection(currentIndex);
                                createListPoint();
                                setScalePlot();
                                resetCount();
                                countSeries = 0;
                            }
                        }
                    }

                    consumed = true;
                }
                xCoordinateEditText.requestFocus();

                // Show the numeric keyboard explicitly
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(xCoordinateEditText, InputMethodManager.SHOW_IMPLICIT);
                xCoordinateEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                return consumed;

            }
        });


        appEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seriesList.size()>1){
                    AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                    builder.setTitle("Данное действие удалит все шаги.");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(valueSet.getText().toString().equals("")){
                                Toast.makeText(ZamerActivity.this, "Выберите точку для изменения", Toast.LENGTH_SHORT).show();
                            }else {
                                Number curX = Float.parseFloat(valueSet.getText().toString());
                                curSeries.setX(curX, curElement);
                                highlightedPoint.setX(curX, 0);
                                updateCountText();
                                calculateDifference();

                                // Delete all elements in seriesList except the current one
                                int currentIndex = seriesSpinner.getSelectedItemPosition();
                                seriesList.clear();
                                seriesList.add(curSeries);
                                seriesSpinner.setSelection(currentIndex);
                                createListPoint();
                                setScalePlot();
                                resetCount();
                                countSeries = 0;
                            }
                        }
                    });
                    builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    AlertDialog ad = builder.create();
                    ad.show();
                }else{
                    if(valueSet.getText().toString().equals("")){
                        Toast.makeText(ZamerActivity.this, "Выберите точку для изменения", Toast.LENGTH_SHORT).show();
                    }else {
                        Number curX = Float.parseFloat(valueSet.getText().toString());
                        curSeries.setX(curX, curElement);
                        highlightedPoint.setX(curX, 0);
                        updateCountText();
                        calculateDifference();

                        // Delete all elements in seriesList except the current one
                        int currentIndex = seriesSpinner.getSelectedItemPosition();
                        seriesList.clear();
                        seriesList.add(curSeries);
                        seriesSpinner.setSelection(currentIndex);
                        createListPoint();
                        setScalePlot();
                        resetCount();
                        countSeries = 0;
                    }
                }
            }
        });


        valueSet.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean consumed = false;
                if (keyCode == KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if(seriesList.size()>1){
                            AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                            builder.setTitle("Данное действие удалит все шаги.");
                            builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(valueSet.getText().toString().equals("")){
                                        Toast.makeText(ZamerActivity.this, "Выберите точку для изменения", Toast.LENGTH_SHORT).show();
                                    }else {
                                        Number curX = Float.parseFloat(valueSet.getText().toString());
                                        curSeries.setX(curX, curElement);
                                        highlightedPoint.setX(curX, 0);
                                        updateCountText();
                                        calculateDifference();

                                        // Delete all elements in seriesList except the current one
                                        int currentIndex = seriesSpinner.getSelectedItemPosition();
                                        seriesList.clear();
                                        seriesList.add(curSeries);
                                        seriesSpinner.setSelection(currentIndex);
                                        createListPoint();
                                        setScalePlot();
                                        resetCount();
                                        countSeries = 0;
                                    }
                                }
                            });
                            builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            AlertDialog ad = builder.create();
                            ad.show();
                        }else{
                            if(valueSet.getText().toString().equals("")){
                                Toast.makeText(ZamerActivity.this, "Выберите точку для изменения", Toast.LENGTH_SHORT).show();
                            }else {
                                Number curX = Float.parseFloat(valueSet.getText().toString());
                                curSeries.setX(curX, curElement);
                                highlightedPoint.setX(curX, 0);
                                updateCountText();
                                calculateDifference();

                                // Delete all elements in seriesList except the current one
                                int currentIndex = seriesSpinner.getSelectedItemPosition();
                                seriesList.clear();
                                seriesList.add(curSeries);
                                seriesSpinner.setSelection(currentIndex);
                                createListPoint();
                                setScalePlot();
                                resetCount();
                                countSeries = 0;
                            }
                        }
                    }

                    consumed = true;
                }
                valueSet.requestFocus();

                // Show the numeric keyboard explicitly
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(valueSet, InputMethodManager.SHOW_IMPLICIT);
                valueSet.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                return consumed;

            }
        });

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentIndex = seriesList.indexOf(curSeries);
                if((currentIndex+1)>= seriesList.size()){
                    count++;
                    Number curX = Float.parseFloat(curSeries.getX(curElement).toString())+1;
                    if(measurementUnitDB.equals("20 м")){
                        if(curElement>=2 && (curSeries.size()-1)-curElement >= 2){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())-0.5;
                            curSeries.setX(curX1, curElement+2);
                            Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())-0.5;
                            curSeries.setX(curX2, curElement-2);
                        }else{if(curElement<2 && (curSeries.size()-1)-curElement >= 2){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())-0.5;
                            curSeries.setX(curX1, curElement+2);
                        }
                            if(curElement>=2 && (curSeries.size()-1)-curElement < 2){
                                Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())-0.5;
                                curSeries.setX(curX2, curElement-2);
                            }
                        }
                    }else{
                        if(curElement>=1 && (curSeries.size()-1)-curElement >= 1){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+1).toString())-0.5;
                            curSeries.setX(curX1, curElement+1);
                            Number curX2 = Float.parseFloat(curSeries.getX(curElement-1).toString())-0.5;
                            curSeries.setX(curX2, curElement-1);
                        }else{if(curElement<1 && (curSeries.size()-1)-curElement >= 1){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+1).toString())-0.5;
                            curSeries.setX(curX1, curElement+2);
                        }
                            if(curElement>=1 && (curSeries.size()-1)-curElement < 1){
                                Number curX2 = Float.parseFloat(curSeries.getX(curElement-1).toString())-0.5;
                                curSeries.setX(curX2, curElement-1);
                            }
                        }
                    }
                    curSeries.setX(curX, curElement);
                    highlightedPoint.setX(curX, 0);

                    List<SimpleXYSeries> subList = seriesList.subList(currentIndex + 1, seriesList.size());
                    subList.clear();
                    updateCountText();
                    calculateDifference();
                    countSeries = currentIndex;
                } else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                    builder.setTitle("Данное действие удалит все шаги.");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            count++;
                            Number curX = Float.parseFloat(curSeries.getX(curElement).toString())+1;
                            if(curElement>=2 && (curSeries.size()-1)-curElement >= 2){
                                Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())-0.5;
                                curSeries.setX(curX1, curElement+2);
                                Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())-0.5;
                                curSeries.setX(curX2, curElement-2);
                            }else{if(curElement<2 && (curSeries.size()-1)-curElement >= 2){
                                Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())-0.5;
                                curSeries.setX(curX1, curElement+2);
                            }
                                if(curElement>=2 && (curSeries.size()-1)-curElement < 2){
                                    Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())-0.5;
                                    curSeries.setX(curX2, curElement-2);
                                }
                            }
                            curSeries.setX(curX, curElement);
                            highlightedPoint.setX(curX, 0);

                            List<SimpleXYSeries> subList = seriesList.subList(currentIndex + 1, seriesList.size());
                            subList.clear();
                            updateCountText();
                            calculateDifference();
                            countSeries = currentIndex;
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
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                int currentIndex = seriesList.indexOf(curSeries);
                if((currentIndex+1)>= seriesList.size()){
                    count--;
                    Number curX = Float.parseFloat(curSeries.getX(curElement).toString())-1;
                    if(measurementUnitDB.equals("20 м")){
                        if(curElement>=2 && (curSeries.size()-1)-curElement >= 2){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())+0.5;
                            curSeries.setX(curX1, curElement+2);
                            Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())+0.5;
                            curSeries.setX(curX2, curElement-2);
                        }else{if(curElement<2 && (curSeries.size()-1)-curElement >= 2){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())+0.5;
                            curSeries.setX(curX1, curElement+2);
                        }
                            if(curElement>=2 && (curSeries.size()-1)-curElement < 2){
                                Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())+0.5;
                                curSeries.setX(curX2, curElement-2);
                            }
                        }
                    }else{
                        if(curElement>=1 && (curSeries.size()-1)-curElement >= 1){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+1).toString())+0.5;
                            curSeries.setX(curX1, curElement+1);
                            Number curX2 = Float.parseFloat(curSeries.getX(curElement-1).toString())+0.5;
                            curSeries.setX(curX2, curElement-1);
                        }else{if(curElement<1 && (curSeries.size()-1)-curElement >= 1){
                            Number curX1 = Float.parseFloat(curSeries.getX(curElement+1).toString())+0.5;
                            curSeries.setX(curX1, curElement+1);
                        }
                            if(curElement>=1 && (curSeries.size()-1)-curElement < 1){
                                Number curX2 = Float.parseFloat(curSeries.getX(curElement-1).toString())+0.5;
                                curSeries.setX(curX2, curElement-1);
                            }
                        }
                    }
                    curSeries.setX(curX, curElement);
                    highlightedPoint.setX(curX, 0);
                    List<SimpleXYSeries> subList = seriesList.subList(currentIndex + 1, seriesList.size());
                    subList.clear();
                    updateCountText();
                    calculateDifference();
                    countSeries = currentIndex;
                } else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                    builder.setTitle("Данное действие удалит все шаги.");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            count--;
                            Number curX = Float.parseFloat(curSeries.getX(curElement).toString())-1;
                            if(curElement>=2 && (curSeries.size()-1)-curElement >= 2){
                                Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())+0.5;
                                curSeries.setX(curX1, curElement+2);
                                Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())+0.5;
                                curSeries.setX(curX2, curElement-2);
                            }else{if(curElement<2 && (curSeries.size()-1)-curElement >= 2){
                                Number curX1 = Float.parseFloat(curSeries.getX(curElement+2).toString())+0.5;
                                curSeries.setX(curX1, curElement+2);
                            }
                                if(curElement>=2 && (curSeries.size()-1)-curElement < 2){
                                    Number curX2 = Float.parseFloat(curSeries.getX(curElement-2).toString())+0.5;
                                    curSeries.setX(curX2, curElement-2);
                                }
                            }
                            curSeries.setX(curX, curElement);
                            highlightedPoint.setX(curX, 0);
                            List<SimpleXYSeries> subList = seriesList.subList(currentIndex + 1, seriesList.size());
                            subList.clear();
                            updateCountText();
                            calculateDifference();
                            countSeries = currentIndex;
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
            }
        });


        createNewSeriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSeriesIndex++;
                if (seriesList.size() > 0) {
                    createNewSeries(curSeries);
//                    Toast.makeText(ZamerActivity.this, ""+getSeriesArray().length, Toast.LENGTH_SHORT).show();
                    seriesSpinner.setSelection(seriesList.size()-1);
                    curSeries = seriesList.get(selectedSeriesIndex);
                    plot.clear();
                    if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                        plot.addSeries(curSeries, seriesFormat);
                    }else{
                        plot.addSeries(curSeries, seriesFormatPromer);
                    }
                    plot.redraw();
                }
                createListPoint();

                resetCount();
                if(isNew){
                   saveDataToDatabase();
                }else{
//                    Toast.makeText(ZamerActivity.this, "Id объекта"+ measurementDB.getId(), Toast.LENGTH_SHORT).show();
                    updateMeasurement(measurementDB);
                }
                isNew = false;
            }
        });



        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSeriesIndex--;
                if (selectedSeriesIndex < 0) {
                    selectedSeriesIndex = seriesList.size() - 1;
                }
                seriesSpinner.setSelection(selectedSeriesIndex);
                curSeries = seriesList.get(selectedSeriesIndex);
                createListPoint();
                plot.clear();
                if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                    plot.addSeries(curSeries, seriesFormat);
                }else{
                    plot.addSeries(curSeries, seriesFormatPromer);
                }
                resetCount();
                plot.redraw();
//                Toast.makeText(ZamerActivity.this, "Счётчик"+countSeries, Toast.LENGTH_SHORT).show();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSeriesIndex++;
                if (selectedSeriesIndex >= seriesList.size()) {
                    selectedSeriesIndex = 0;
                }
                seriesSpinner.setSelection(selectedSeriesIndex);
                curSeries = seriesList.get(selectedSeriesIndex);
                createListPoint();
                plot.clear();
                if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                    plot.addSeries(curSeries, seriesFormat);
                }else{
                    plot.addSeries(curSeries, seriesFormatPromer);
                }
                resetCount();
                plot.redraw();
//                Toast.makeText(ZamerActivity.this, "Счётчик"+countSeries, Toast.LENGTH_SHORT).show();
            }
        });


        clearListPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                builder.setTitle("Данное действие удалит все шаги.");
                builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        curSeries.clear();
                        int currentIndex = seriesSpinner.getSelectedItemPosition();
                        seriesList.clear();
                        seriesList.add(curSeries);
                        seriesSpinner.setSelection(currentIndex);
                        createListPoint();
//                      setScalePlot();
                        resetCount();
                        countPoint = 1;
                        countSeries = 0;
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


        resetChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentIndex = seriesList.indexOf(curSeries);
                List<SimpleXYSeries> subList = seriesList.subList(currentIndex , seriesList.size());
                subList.clear();
                countSeries = currentIndex-1;
                createNewSeries(seriesList.get(countSeries));
                seriesSpinner.setSelection(seriesList.size()-1);
                curSeries = seriesList.get(seriesList.size()-1);
                updateCountText();
                createListPoint();
                calculateDifference();
                if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                    plot.clear();
                    plot.addSeries(curSeries, seriesFormat);
                }else{
                    plot.clear();
                    plot.addSeries(curSeries, seriesFormatPromer);
                }
            }
        });

    }

    private long saveDataToDatabase() {
        class SaveDataToDatabase extends AsyncTask<Void, Void, Long> {
            @Override
            protected Long doInBackground(Void... voids) {
                Gson gson = new Gson();
                String seriesListJson = gson.toJson(seriesList);

                Measurement measurement = new Measurement();
                measurement.setName(zamerNameDB);
                measurement.setCountPoint(countPoint);
                measurement.setCountSeries(countSeries);
                measurement.setMeasurementUnit(measurementUnitDB);
                measurement.setSeriesListJson(seriesListJson);
                long insertedId = AppDatabase.getDatabase(getApplicationContext())
                        .measurementDao()
                        .insertAndReturnId(measurement);

                return insertedId;
            }

            @Override
            protected void onPostExecute(Long insertedId) {
                super.onPostExecute(insertedId);
                // Handle the result here
                // For example, you can display a toast message or update the UI
                Toast.makeText(ZamerActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();

                // Get the inserted measurement on a background thread
                new AsyncTask<Void, Void, Measurement>() {
                    @Override
                    protected Measurement doInBackground(Void... voids) {
                        return measurementDao.getMeasurementById(insertedId);
                    }

                    @Override
                    protected void onPostExecute(Measurement measurement) {
                        super.onPostExecute(measurement);
                        // Update the measurementDB variable with the inserted measurement
                        measurementDB = measurement;
                    }
                }.execute();
            }
        }
        SaveDataToDatabase sM = new SaveDataToDatabase();
        sM.execute();
        try {
            return sM.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }
    private void updateMeasurement(final Measurement measurementDB) {

        class UpdateMeasurement extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {

                Gson gson = new Gson();
                String seriesListJson = gson.toJson(seriesList);
                measurementDB.setCountPoint(countPoint);
                measurementDB.setCountSeries(countSeries);
                measurementDB.setSeriesListJson(seriesListJson);
                AppDatabase.getDatabase(getApplicationContext())
                        .measurementDao()
                        .updateMeasurement(measurementDB);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }
        UpdateMeasurement uM = new UpdateMeasurement();
        uM.execute();
    }




    //Метод добавление точки в список
    private void addDataPoint(float xCoordinate, float yCoordinate) {
        // Create a new data point and add it to the series
        curSeries.addLast(xCoordinate, yCoordinate);

        // Notify the adapter about the changes
        createListPoint();
        setScalePlot();
        // Refresh the plot
        plot.redraw();
        countPoint++;
    }

    //Метод нажатия на элементы списка
    public void onItemClick(String number, String value, int index) {

        if(seriesSpinner.getSelectedItemPosition()==0 || seriesList.size() == 1){
            blockEdit.setVisibility(View.VISIBLE);
            TextView numberEdit = findViewById(R.id.numberEdit);
            numberEdit.setText("Измерение №"+ number);
            valueSet.setText(value);
            // Create a new data point with the X value as the current count and the Y value as the clicked item's value
            plot.clear();
            highlightedPoint = new SimpleXYSeries("");
            highlightedPoint.addFirst(Float.parseFloat(value),Float.parseFloat(number));
            if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                plot.addSeries(curSeries, seriesFormat);
            }else{
                plot.addSeries(curSeries, seriesFormatPromer);
            }
            plot.addSeries(highlightedPoint, pointFormat);
            plot.redraw();
            curElement = index;
            resetCount();
        }else{
            blockEdit.setVisibility(View.GONE);
            TextView numberPP = findViewById(R.id.numberPP);
            numberPP.setText("Измерение №"+ number);
            TextView znach = findViewById(R.id.znach);
            znach.setText(value);
            // Create a new data point with the X value as the current count and the Y value as the clicked item's value
            plot.clear();
            highlightedPoint = new SimpleXYSeries("");
            highlightedPoint.addFirst(Double.parseDouble(value),Double.parseDouble(number));
            if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0){
                plot.addSeries(curSeries, seriesFormat);
            }else{
                plot.addSeries(curSeries, seriesFormatPromer);
            }
            plot.addSeries(highlightedPoint, pointFormat);
            plot.redraw();
            curElement = index;
            resetCount();
        }

    }

    //Метод обновления счётчика
    private void updateCountText() {
        countTextView.setText(String.valueOf(count));
        createListPoint();
        plot.redraw();
    }

    //Метод создания новых списков
    private void createNewSeries(SimpleXYSeries previousSeries) {
        SimpleXYSeries newSeries = new SimpleXYSeries("Шаг " + (countSeries+1));
        for (int i = 0; i < previousSeries.size(); i++) {
            double x = previousSeries.getX(i).doubleValue();
            double y = previousSeries.getY(i).doubleValue();
            newSeries.addLast(x , y); // Add 1 to the X value to create a new series based on the previous one
        }
        seriesList.add(newSeries); // Add the new series to the list
        plot.redraw();
        // Update the spinner adapter with the new series
        String[] newSeriesArray = getSeriesArray();
        ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, newSeriesArray);
        newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seriesSpinner.setAdapter(newSeriesAdapter);
        createListPoint();
        countSeries++;
    }

    private String[] getSeriesArray() {
        String[] seriesArray = new String[seriesList.size()];
        seriesArray[0] = "Промер";
        for (int i = 1; i < seriesList.size(); i++) {
            seriesArray[i] = "Шаг " + (i);
        }
        return seriesArray;
    }

    //Метод сравнения двух списков
    private SimpleXYSeries calculateDifference() {
        SimpleXYSeries differenceSeries = new SimpleXYSeries("Difference");

        if (seriesList.size() > 1 && selectedSeriesIndex > 0) {
            // Get the two selected series from the spinner
            SimpleXYSeries series1 = seriesList.get(selectedSeriesIndex - 1);
            SimpleXYSeries series2 = seriesList.get(selectedSeriesIndex);
            // Calculate the difference between the two series and add it to the difference series
            for (int i = 0; i < series1.size(); i++) {
                double y = series1.getY(i).doubleValue();
                double x1 = series1.getX(i).doubleValue();
                double x2 = series2.getX(i).doubleValue();
                double difference = x2 - x1;
                differenceSeries.addLast(difference, y);

            }
            // Calculate the difference between the two series and display it
        }
        return differenceSeries;
    }

    private void createListPoint(){
       PointAdapter pointAdapter;
       PointAdapterForDiff pointAdapterForDiff;
        if(seriesSpinner.getSelectedItemPosition()==0 || seriesList.size() == 1){
            pointAdapter = new PointAdapter(ZamerActivity.this, curSeries, calculateDifference() , ZamerActivity.this);
            recyclerView.setAdapter(pointAdapter);
            blockSdvig.setVisibility(View.GONE);
            blockEdit.setVisibility(View.GONE);
            blockAddPoint.setVisibility(View.VISIBLE);
            clearListPoint.setVisibility(View.VISIBLE);
            resetChanges.setVisibility(View.GONE);

        }else{
            pointAdapterForDiff = new PointAdapterForDiff(ZamerActivity.this, curSeries, calculateDifference() , ZamerActivity.this);
            recyclerView.setAdapter(pointAdapterForDiff);
            blockSdvig.setVisibility(View.VISIBLE);
            blockEdit.setVisibility(View.GONE);
            blockAddPoint.setVisibility(View.GONE);
            clearListPoint.setVisibility(View.GONE);
            resetChanges.setVisibility(View.VISIBLE);
        }
        plot.redraw();
    }

    private void setScalePlot(){
        double maxXValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < curSeries.size(); i++) {
            double x = Double.parseDouble(curSeries.getX(i).toString());
            if (x > maxXValue) {
                maxXValue = x;
            }
        }

        double minXValue = Double.POSITIVE_INFINITY;
        for (int i = 0; i < curSeries.size(); i++) {
            double x = Double.parseDouble(curSeries.getX(i).toString());
            if (x < minXValue) {
                minXValue = x;
            }
        }
//        Toast.makeText(this, "max"+maxXValue+" min"+minXValue , Toast.LENGTH_SHORT).show();
        Number minValueY = 0;
        Number maxValueY = Float.parseFloat(curSeries.getY(curSeries.size()-1).toString()) * 1.2;
        Number minValueX = minXValue;
        Number maxValueX = maxXValue;
        plot.setRangeBoundaries(minValueY, maxValueY, BoundaryMode.FIXED);
        plot.setDomainBoundaries(minValueX, maxValueX, BoundaryMode.FIXED);
        plot.redraw();
    }

    private void resetCount(){
        count = 0;
        countTextView.setText(String.valueOf(count));
    }


    private Measurement getMeasurement(Integer meastId) {
        class GetMeasurement extends AsyncTask<Integer, Void, Measurement> {

            @Override
            protected Measurement doInBackground(Integer ... params) {
                Measurement measurement = AppDatabase
                        .getDatabase(getApplicationContext())
                        .measurementDao()
                        .getMeasurementById(meastId);
                return measurement;
            }
            @Override
            protected void onPostExecute(Measurement measurement) {
                super.onPostExecute(measurement);
            }
        }
        GetMeasurement gM = new GetMeasurement();
        gM.execute(meastId);
        try {
            measurementDB = gM.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return measurementDB;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                Intent intent = new Intent(ZamerActivity.this, MainActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void onBackPressed() {
        Intent intent = new Intent(ZamerActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }



}