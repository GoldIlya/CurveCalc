package ru.itinbiz.curvecalc;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.view.KeyEvent.KEYCODE_ENTER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ru.itinbiz.curvecalc.adapter.PointAdapter;
import ru.itinbiz.curvecalc.adapter.PointAdapterForDiff;
import ru.itinbiz.curvecalc.data.AppDatabase;
import ru.itinbiz.curvecalc.data.MeasurementDao;
import ru.itinbiz.curvecalc.model.Measurement;
import ru.itinbiz.curvecalc.model.PointShift;
import ru.itinbiz.curvecalc.service.MyLineAndPointFormatter;

public class ZamerActivity extends AppCompatActivity implements PointAdapter.OnItemClickListener, PointAdapterForDiff.OnItemClickListener {

    int measurementIdDb;
    boolean isNew = false;
    boolean isModeOnePoint = true;
    boolean isModeEnter;
    boolean isClick = false;
    private XYPlot plot;
    private SimpleXYSeries series, curSeries, curSeriesInt, curSeriesDouble,  highlightedPoint;
    private RecyclerView recyclerView;
    private LineAndPointFormatter seriesFormat, seriesFormatInt,
            seriesFormatDouble, seriesFormatPromer,
            seriesFormatPromerInt, seriesFormatPromerDouble,
            pointFormat, pointFormatInt, pointFormatDouble;
    private List<SimpleXYSeries> seriesList = new ArrayList<>();
    private Map<Integer, Integer> curElements = new HashMap<>();
    private int curElement = -1;
    private int count = 0;
    private double countPoint = 1;
    private int countSeries = 0;

    private LinearLayout blockSdvig, blockSeries, blockEdit, blockAddPoint;
    private Button btnPlus, btnMinus;
    private FloatingActionButton  btnEnterVal, clearListPoint, createNewSeriesButton, prevButton, nextButton, resetChanges, btnTable, appEdit;
    private EditText etEnterVal, valueSet;
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
    private Handler handler = new Handler();
    private Handler handler1 = new Handler();

    private Map<Integer, Integer> pointShiftMap = new HashMap<>();



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
        btnEnterVal = findViewById(R.id.btnEnterVal);
        etEnterVal = findViewById(R.id.etEnterVal);
        valueSet = findViewById(R.id.valueSet);
        appEdit = findViewById(R.id.appEdit);
        btnPlus = findViewById(R.id.increment_button);
        btnMinus = findViewById(R.id.decrement_button);
//        btnShiftMode = findViewById(R.id.btnShiftMode);
        prevButton = findViewById(R.id.prev_button);
        nextButton = findViewById(R.id.next_button);
        createNewSeriesButton = findViewById(R.id.create_new_series_button);
        clearListPoint = findViewById(R.id.clearListPoint);
        resetChanges = findViewById(R.id.resetChange);
        btnTable = findViewById(R.id.btnTable);
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
            blockSdvig.setVisibility(View.VISIBLE);
            blockEdit.setVisibility(View.GONE);
            blockAddPoint.setVisibility(View.GONE);
            clearListPoint.setVisibility(View.GONE);
            resetChanges.setVisibility(View.VISIBLE);
        }

        recyclerView = findViewById(R.id.listPoint);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
            Type type = new TypeToken<Map<Integer, Integer>>() {}.getType();
            curElements = gson.fromJson(measurementDB.getCurElementsJson(), type);
            if(curElements == null){
                curElements = new HashMap<>();
            }
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
            if(curElements == null){
                curElements = new HashMap<>();
            }
        }

        countTextView = findViewById(R.id.count_text_view);


        curSeries = seriesList.get(0);

        // Создаём разные форматы для отображения на графике
        seriesFormat = new MyLineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        seriesFormat.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeries, int index) {
                return "";
            }
        });


        seriesFormatInt = new MyLineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        seriesFormatInt.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeriesInt, int index) {
                return "";
            }
        });


        seriesFormatDouble = new MyLineAndPointFormatter(this, R.xml.linepunktir_point_formatter_with_labels);
        seriesFormatDouble.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeriesDouble, int index) {
                return "";
            }
        });

        seriesFormatPromer = new MyLineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
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


        seriesFormatPromerInt = new MyLineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);
        seriesFormatPromerInt.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeriesInt, int index) {

                if(calculateDiffInt().getX(index).floatValue() == 0.0){
                    return "";
                }else{
                    return ""+calculateDiffInt().getX(index).floatValue();
                }
            }
        });

        seriesFormatPromerDouble = new MyLineAndPointFormatter(this, R.xml.linepunktir_point_formatter_with_labels);

        seriesFormatPromerDouble.setPointLabeler(new PointLabeler() {
            @Override

            public String getLabel(XYSeries curSeriesDouble, int index) {
                if(calculateDiffDouble().getX(index).floatValue() == 0.0){
                    return "";
                }else{
                    return ""+calculateDiffDouble().getX(index).floatValue();
                }
            }
        });


        seriesFormatDouble.getLinePaint().setPathEffect(new DashPathEffect(new float[] {
                // always use DP when specifying pixel sizes, to keep things consistent across devices:
                PixelUtils.dpToPix(10),
                PixelUtils.dpToPix(5)}, 0));

        seriesFormatPromerDouble.getLinePaint().setPathEffect(new DashPathEffect(new float[] {
                // always use DP when specifying pixel sizes, to keep things consistent across devices:
                PixelUtils.dpToPix(10),
                PixelUtils.dpToPix(5)}, 0));



        pointFormat = new LineAndPointFormatter(this, R.xml.formatter_point);
        pointFormat.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeries, int index) {
//                return ""+calculateDifference().getX(curElement).floatValue();
                return "";
            }
        });


        pointFormatInt = new LineAndPointFormatter(this, R.xml.formatter_point);
        pointFormatInt.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeriesInt, int index) {
//                return ""+calculateDifference().getX(curElement).floatValue();
                return "";
            }
        });

        pointFormatDouble = new LineAndPointFormatter(this, R.xml.formatter_point);
        pointFormatDouble.setPointLabeler(new PointLabeler() {
            @Override
            public String getLabel(XYSeries curSeriesDouble, int index) {
//                return ""+calculateDifference().getX(curElement).floatValue();
                return "";
            }
        });


        // Add the series to the plot with the formatter
        if(measurementUnitDB.equals("Точки и полуточки 2")){
            curSeriesInt = new SimpleXYSeries("1");
            curSeriesDouble = new SimpleXYSeries("1/2");
            pointAndDoublePoint();
        }else{
            if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                plot.addSeries(curSeries, seriesFormat);
            }else{
                plot.addSeries(curSeries, seriesFormatPromer);
            }
        }

        // Set the plot's properties
        plot.setRangeLabel("Y");
        plot.setDomainLabel("X");
        setScalePlot();
        plot.setDomainStep(StepMode.INCREMENT_BY_PIXELS,30);
        plot.setRangeStep(StepMode.INCREMENT_BY_PIXELS,50);
        PanZoom.attach(plot, PanZoom.Pan.BOTH, PanZoom.Zoom.SCALE);
        calculateDifference();
        createListPoint();


        seriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onSeriesSpinnerItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        btnEnterVal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String xCoordinateString = etEnterVal.getText().toString();
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
                                String[] newSeriesArray = getSeriesArray();
                                ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
                                newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                seriesSpinner.setAdapter(newSeriesAdapter);
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
                if(measurementUnitDB.equals("Точки и полуточки 2")){
                    pointAndDoublePoint();
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (!imm.isActive(etEnterVal)) {
                    imm.showSoftInput(etEnterVal, InputMethodManager.SHOW_IMPLICIT);
                }
                if(isModeEnter){
                    etEnterVal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }else{
                    etEnterVal.setInputType(TYPE_CLASS_NUMBER);
                }
            }
        });


        etEnterVal.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean consumed = false;
                if (keyCode == KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        String xCoordinateString = etEnterVal.getText().toString();
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
                                        if(measurementUnitDB.equals("Точки и полуточки 2")){
                                            pointAndDoublePoint();
                                        }
                                        setScalePlot();
                                        resetCount();
                                        String[] newSeriesArray = getSeriesArray();
                                        ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
                                        newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        seriesSpinner.setAdapter(newSeriesAdapter);
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
                                if(measurementUnitDB.equals("Точки и полуточки 2")){
                                    pointAndDoublePoint();
                                }
                                setScalePlot();
                                resetCount();
                                countSeries = 0;
                            }
                        }
                    }

                    consumed = true;
                }
                // Show the numeric keyboard explicitly
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (!imm.isActive(etEnterVal)) {
                    imm.showSoftInput(etEnterVal, InputMethodManager.SHOW_IMPLICIT);
                }
                if(isModeEnter){
                    etEnterVal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }else{
                    etEnterVal.setInputType(TYPE_CLASS_NUMBER);
                }
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
                            editPoint();
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
                    editPoint();
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
                                    editPoint();
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
                            editPoint();
                        }
                    }

                    consumed = true;
                }
                valueSet.requestFocus();

                // Show the numeric keyboard explicitly
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(valueSet, InputMethodManager.SHOW_IMPLICIT);
                if(isModeEnter){
                    valueSet.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }else{
                    valueSet.setInputType(TYPE_CLASS_NUMBER);
                }
                return consumed;

            }
        });

        btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    shiftAndReset(1);
            }
        });

        btnPlus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                handler.post(runnable); // start calling plusPoint() repeatedly
                return true;
            }
        });

        btnPlus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    handler.removeCallbacks(runnable); // stop calling plusPoint() when button is released
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int currentIndex = seriesList.indexOf(curSeries);
                    if ((currentIndex + 1) >= seriesList.size()) {
                    } else {
                        showAlertDialog("Данное действие удалит все шаги.", -1);
                    }
                }
                return false;
            }
        });

        btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    shiftAndReset(-1);

            }
        });


        btnMinus.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                handler.post(runnable1); // start calling plusPoint() repeatedly
                return true;
            }
        });

        btnMinus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    handler1.removeCallbacks(runnable1); // stop calling plusPoint() when button is released
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int currentIndex = seriesList.indexOf(curSeries);
                    if ((currentIndex + 1) >= seriesList.size()) {
                    } else {
                        showAlertDialog("Данное действие удалит все шаги.", -1);
                    }
                }
                return false;
            }
        });

        createNewSeriesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((seriesList.indexOf(curSeries) + 1) >= seriesList.size()) {
                    createNewSeriesAndRefresh();
                } else {
                    showConfirmDialog();
                }
                hideKeyboard();
                curElement = -1;
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
                if(measurementUnitDB.equals("Точки и полуточки 2")){
                    pointAndDoublePoint();
                }else{
                    if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                        plot.addSeries(curSeries, seriesFormat);
                    }else{
                        plot.addSeries(curSeries, seriesFormatPromer);
                    }
                }
                resetCount();
                hideKeyboard();
                plot.redraw();
                if(curElements.get(selectedSeriesIndex) == null){
                    curElement = -1;
                }else{
                    curElement = curElements.get(selectedSeriesIndex).intValue();
                }
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
                if(measurementUnitDB.equals("Точки и полуточки 2")){
                    pointAndDoublePoint();
                }else{
                    if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                        plot.addSeries(curSeries, seriesFormat);
                    }else{
                        plot.addSeries(curSeries, seriesFormatPromer);
                    }
                }
                resetCount();
                hideKeyboard();
                plot.redraw();
//
                if(curElements.get(selectedSeriesIndex) == null){
                    curElement = -1;
                }else{
                    curElement = curElements.get(selectedSeriesIndex).intValue();
                }
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
                        createListPoint();
//                      setScalePlot();
                        resetCount();
                        plot.clear();
                        String[] newSeriesArray = getSeriesArray();
                        ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
                        newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        seriesSpinner.setAdapter(newSeriesAdapter);
                        seriesSpinner.setSelection(0);
                        if(measurementUnitDB.equals("Точки и полуточки 2")){
                            pointAndDoublePoint();
                        }else{
                            if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                                plot.addSeries(curSeries, seriesFormat);
                            }else{
                                plot.addSeries(curSeries, seriesFormatPromer);
                            }
                        }
                        countPoint = 1;
                        countSeries = 0;
                        plot.redraw();
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
                if((currentIndex+1)>= seriesList.size()){
                    resetChangesList();
                    resetCountBtn();
                }else{

                    AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                    builder.setTitle("Данное действие удалит все шаги.");
                    builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            resetChangesList();
                            resetCountBtn();
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

        btnTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
                builder.setTitle("Выберите действие");
                builder.setPositiveButton("Открыть таблицу", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ZamerActivity.this, SeriesTableActivity.class);
                        Gson gson = new Gson();
                        String seriesListJson = gson.toJson(seriesList);
                        String curElementsToJson = gson.toJson(curElements);
                        String nameZamer = zamerNameDB.toString();
                        intent.putExtra("seriesListJson", seriesListJson).putExtra("nameZamer", nameZamer).putExtra("curElementsToJson", curElementsToJson);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("Выбрать режим", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Inflate the custom layout
                        View view = LayoutInflater.from(ZamerActivity.this).inflate(R.layout.dialog_input_mode, null);

                        // Get the EditText and RadioGroup from the custom layout
                        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
                        Switch swEnterMinusVal = view.findViewById(R.id.enterMinusVal);
                        if(!isModeEnter){
                            swEnterMinusVal.setChecked(false);
                        }else{
                            swEnterMinusVal.setChecked(true);
                        }
                        // Set the default selection for the RadioGroup
                        if(isModeOnePoint){
                            radioGroup.check(R.id.radioBtnOnePoint);
                        }else{
                            radioGroup.check(R.id.radioBtnAllPoint);
                        }

                        // Create the AlertDialog with the custom layout
                        AlertDialog dialogWithInput = new AlertDialog.Builder(ZamerActivity.this)
                                .setTitle("Выбрать режим")
                                .setView(view)
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int selectedId = radioGroup.getCheckedRadioButtonId();
                                        RadioButton selectedRadioButton = view.findViewById(selectedId);
                                        String shiftMode = selectedRadioButton.getText().toString();
                                        Toast.makeText(ZamerActivity.this, "id режима"+ shiftMode, Toast.LENGTH_SHORT).show();
                                        if(shiftMode.equals("режим включает возможность сдвига всех точек в шаге")){
                                            isModeOnePoint = false;
                                        }else{
                                            isModeOnePoint = true;
                                        }
                                        if(swEnterMinusVal.isChecked()){
                                            isModeEnter = true;
                                            etEnterVal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                                            valueSet.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                                        }else{
                                            isModeEnter = false;
                                            etEnterVal.setInputType(TYPE_CLASS_NUMBER);
                                            valueSet.setInputType(TYPE_CLASS_NUMBER);
                                        }


                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .create();

                        dialogWithInput.show();
                    }
                });
                builder.create().show();

            }
        });

//
    }

    private long saveDataToDatabase() {
        class SaveDataToDatabase extends AsyncTask<Void, Void, Long> {
            @Override
            protected Long doInBackground(Void... voids) {
                Gson gson = new Gson();
                String seriesListJson = gson.toJson(seriesList);
                String curElementsJson = gson.toJson(curElements);

                Measurement measurement = new Measurement();
                measurement.setName(zamerNameDB);
                measurement.setCountPoint(countPoint);
                measurement.setCountSeries(countSeries);
                measurement.setMeasurementUnit(measurementUnitDB);
                measurement.setSeriesListJson(seriesListJson);
                measurement.setCurElementsJson(curElementsJson);

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
                String curElementsJson = gson.toJson(curElements);

                measurementDB.setCountPoint(countPoint);
                measurementDB.setCountSeries(countSeries);
                measurementDB.setSeriesListJson(seriesListJson);
                measurementDB.setCurElementsJson(curElementsJson);

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
        if(measurementUnitDB.equals("Точки и полуточки 2")){
            countPoint = countPoint+0.5;
        }else{
            countPoint++;
        }
        int lastPosition = recyclerView.getAdapter().getItemCount()-1;
        recyclerView.scrollToPosition(lastPosition);
        etEnterVal.setText("");
    }

    //Метод нажатия на элементы списка
    public void onItemClick(String number,Double numberDouble, String value, int index) {
            isClick = true;
        if(seriesSpinner.getSelectedItemPosition()==0 || seriesList.size() == 1){
            blockEdit.setVisibility(View.VISIBLE);
            TextView numberEdit = findViewById(R.id.numberEdit);
            Double roundValue = Double.parseDouble(value);
            numberEdit.setText(" № "+ number+" ");
            valueSet.setText(String.valueOf(Math.round(roundValue)));
            // Create a new data point with the X value as the current count and the Y value as the clicked item's value
            plot.clear();
            highlightedPoint = new SimpleXYSeries("");
            highlightedPoint.addFirst(Float.parseFloat(value),numberDouble);
            if(measurementUnitDB.equals("Точки и полуточки 2")){
                pointAndDoublePoint();
            }else{
                if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                    plot.addSeries(curSeries, seriesFormat);
                }else{
                    plot.addSeries(curSeries, seriesFormatPromer);
                }
            }
            plot.addSeries(highlightedPoint, pointFormat);
            plot.redraw();
            curElement = index;
            curElements.put(selectedSeriesIndex, curElement); // Add the current series to the curElements map
            resetCount();
        }else{
            blockSdvig.setVisibility(View.VISIBLE);
            blockEdit.setVisibility(View.GONE);
            TextView numberPP = findViewById(R.id.numberPP);
            numberPP.setText(" № "+ number+" ");
            TextView znach = findViewById(R.id.znach);
            znach.setText(value);
            // Create a new data point with the X value as the current count and the Y value as the clicked item's value
            plot.clear();
            highlightedPoint = new SimpleXYSeries("");
            highlightedPoint.addFirst(Double.parseDouble(value),numberDouble);
            if(measurementUnitDB.equals("Точки и полуточки 2")){
                pointAndDoublePoint();
            }else{
                if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                    plot.addSeries(curSeries, seriesFormat);
                }else{
                    plot.addSeries(curSeries, seriesFormatPromer);
                }
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

    private SimpleXYSeries calculateDiffInt() {
        SimpleXYSeries differenceSeries = new SimpleXYSeries("Difference");
        SimpleXYSeries series1Int = new SimpleXYSeries("DiffInt");
        SimpleXYSeries series2Int = new SimpleXYSeries("DiffInt");

        if (seriesList.size() > 1 && selectedSeriesIndex > 0) {
            // Get the two selected series from the spinner
            SimpleXYSeries series1 = seriesList.get(selectedSeriesIndex - 1);
            for (int i = 0; i < series1.size(); i++) {
                double yCurent = series1.getY(i).doubleValue();
                boolean isInteger = (yCurent - Math.floor(yCurent)) == 0;
                if (isInteger) {
                    double x = series1.getX(i).doubleValue();
                    double y = series1.getY(i).doubleValue();
                    series1Int.addLast(x,y);
                }
            }
            SimpleXYSeries series2 = seriesList.get(selectedSeriesIndex);
            for (int i = 0; i < series2.size(); i++) {
                double yCurent = series2.getY(i).doubleValue();
                boolean isInteger = (yCurent - Math.floor(yCurent)) == 0;
                if (isInteger) {
                    double x = series2.getX(i).doubleValue();
                    double y = series2.getY(i).doubleValue();
                    series2Int.addLast(x,y);
                }
            }
            // Calculate the difference between the two series and add it to the difference series
            for (int i = 0; i < series1Int.size(); i++) {
                double y = series1Int.getY(i).doubleValue();
                double x1 = series1Int.getX(i).doubleValue();
                double x2 = series2Int.getX(i).doubleValue();
                double difference = x2 - x1;
                differenceSeries.addLast(difference, y);

            }
            // Calculate the difference between the two series and display it
        }
        return differenceSeries;
    }

    private SimpleXYSeries calculateDiffDouble() {
        SimpleXYSeries differenceSeries = new SimpleXYSeries("Difference");
        SimpleXYSeries series1Double = new SimpleXYSeries("DiffDouble");
        SimpleXYSeries series2Double = new SimpleXYSeries("DiffDouble");

        if (seriesList.size() > 1 && selectedSeriesIndex > 0) {
            // Get the two selected series from the spinner
            SimpleXYSeries series1 = seriesList.get(selectedSeriesIndex - 1);
            for (int i = 0; i < series1.size(); i++) {
                double yCurent = series1.getY(i).doubleValue();
                boolean isInteger = (yCurent - Math.floor(yCurent)) == 0;
                if (!isInteger) {
                    double x = series1.getX(i).doubleValue();
                    double y = series1.getY(i).doubleValue();
                    series1Double.addLast(x,y);
                }
            }
            SimpleXYSeries series2 = seriesList.get(selectedSeriesIndex);
            for (int i = 0; i < series2.size(); i++) {
                double yCurent = series2.getY(i).doubleValue();
                boolean isInteger = (yCurent - Math.floor(yCurent)) == 0;
                if (!isInteger) {
                    double x = series2.getX(i).doubleValue();
                    double y = series2.getY(i).doubleValue();
                    series2Double.addLast(x,y);
                }
            }
            // Calculate the difference between the two series and add it to the difference series
            for (int i = 0; i < series1Double.size(); i++) {
                double y = series1Double.getY(i).doubleValue();
                double x1 = series1Double.getX(i).doubleValue();
                double x2 = series2Double.getX(i).doubleValue();
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
//            btnShiftMode.setVisibility(View.GONE);
            clearListPoint.setVisibility(View.VISIBLE);
            resetChanges.setVisibility(View.GONE);


        }else{
            pointAdapterForDiff = new PointAdapterForDiff(ZamerActivity.this, curSeries, calculateDifference() , ZamerActivity.this, curElement, isModeOnePoint, pointShiftMap);
            recyclerView.setAdapter(pointAdapterForDiff);
//            btnShiftMode.setVisibility(View.VISIBLE);
            blockEdit.setVisibility(View.GONE);
            blockAddPoint.setVisibility(View.GONE);
            clearListPoint.setVisibility(View.GONE);
            blockSdvig.setVisibility(View.VISIBLE);
            resetChanges.setVisibility(View.VISIBLE);

        }
        plot.redraw();
    }

    private void setScalePlot(){
        double minXValue = Double.POSITIVE_INFINITY;
        double maxXValue = Double.NEGATIVE_INFINITY;
        Number maxValueY;
        if(curSeries.size()==0){
            minXValue = 0;
            maxXValue = 0;
            maxValueY = 0;
        }else{
            maxValueY = Float.parseFloat(curSeries.getY(curSeries.size()-1).toString()) * 1.2;
            for (int i = 0; i < curSeries.size(); i++) {
                double x = Double.parseDouble(curSeries.getX(i).toString());
                if (x > maxXValue) {
                    maxXValue = x;
                }
            }

            for (int i = 0; i < curSeries.size(); i++) {
                double x = Double.parseDouble(curSeries.getX(i).toString());
                if (x < minXValue) {
                    minXValue = x;
                }
            }
        }

//        Toast.makeText(this, "max"+maxXValue+" min"+minXValue , Toast.LENGTH_SHORT).show();
        Number minValueY = 0;
        Number minValueX = minXValue;
        Number maxValueX = maxXValue;
        plot.setRangeBoundaries(minValueY, maxValueY, BoundaryMode.FIXED);
        plot.setDomainBoundaries(minValueX, maxValueX, BoundaryMode.FIXED);
        plot.redraw();
    }

    private void resetCount(){
        if(isClick){
            count = 0;
            countTextView.setText(String.valueOf(count));
        }

    }

    private void resetCountBtn(){
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
                Intent intent = new Intent(ZamerActivity.this, AddZamerActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shiftAndReset(int direction) {
        if (isModeOnePoint) {
            if (isClick) {
                resetChangesList();
                shiftPoint(direction);
                isClick = false;
            } else {
                shiftPoint(direction);
            }
        } else {
            shiftPoint(direction);
        }
    }


    private void showAlertDialog(String title, final int direction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
        builder.setTitle(title);
        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                shiftAndReset(direction);
                String[] newSeriesArray = getSeriesArray();
                ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
                newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                seriesSpinner.setAdapter(newSeriesAdapter);
                seriesSpinner.setSelection(seriesList.indexOf(curSeries));
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



    public void onBackPressed() {
        Intent intent = new Intent(ZamerActivity.this, AddZamerActivity.class);
        startActivity(intent);
        finish();
    }


    private void onSeriesSpinnerItemSelected(int position) {
        selectedSeriesIndex = position;
        curSeries = seriesList.get(selectedSeriesIndex);
        if(curElements.get(position) == null){
            curElement = -1;
        }else{
            curElement = curElements.get(position).intValue();
        }
        createListPoint();
        plot.clear();
        if (measurementUnitDB.equals("Точки и полуточки 2")) {
            pointAndDoublePoint();
        } else {
            if (seriesList.size() == 1 || seriesSpinner.getSelectedItemPosition() == 0) {
                plot.addSeries(curSeries, seriesFormat);
            } else {
                plot.addSeries(curSeries, seriesFormatPromer);
            }
        }
        resetCount();
        hideKeyboard();
        plot.redraw();
    }


    public void pointAndDoublePoint(){
        curSeriesInt.clear();
        curSeriesDouble.clear();
        for (int i = 0; i < curSeries.size(); i++) {
            double yCurent = curSeries.getY(i).doubleValue();
            boolean isInteger = (yCurent - Math.floor(yCurent)) == 0;
            if (isInteger) {
                double x = curSeries.getX(i).doubleValue();
                double y = curSeries.getY(i).doubleValue();
                curSeriesInt.addLast(x,y);
            } else {
                double x = curSeries.getX(i).doubleValue();
                double y = curSeries.getY(i).doubleValue();
                curSeriesDouble.addLast(x,y);
            }
        }


        if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
            plot.clear();
            plot.addSeries(curSeriesInt, seriesFormatInt);
            plot.addSeries(curSeriesDouble, seriesFormatDouble);
        }else{
            plot.clear();
            plot.addSeries(curSeriesInt, seriesFormatPromerInt);
            plot.addSeries(curSeriesDouble, seriesFormatPromerDouble);
        }
        plot.redraw();
    }


    public void editPoint(){
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
            if(measurementUnitDB.equals("Точки и полуточки 2")){
                pointAndDoublePoint();
            }
            String[] newSeriesArray = getSeriesArray();
            ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
            newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            seriesSpinner.setAdapter(newSeriesAdapter);
            setScalePlot();
            resetCount();
            countSeries = 0;
        }
    }

    // отдельный метод для создания новой серии и обновления интерфейса
    private void createNewSeriesAndRefresh() {
        selectedSeriesIndex++;
        createNewSeries(curSeries);
        curSeries = seriesList.get(selectedSeriesIndex);
        refreshSeriesSpinner();
        plot.clear();
        addSeriesToPlot();
        plot.redraw();
        createListPoint();
        resetChangesList();
        resetCountBtn();
        if (isNew) {
            saveDataToDatabase();
        } else {
            updateMeasurement(measurementDB);
        }
        isNew = false;
    }

    // отдельный метод для показа диалога подтверждения
    private void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ZamerActivity.this);
        builder.setTitle("Данное действие удалит все последующие шаги. ");
        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                createNewSeriesAndRefresh();
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

    // отдельный метод для обновления спиннера серий
    private void refreshSeriesSpinner() {
        String[] newSeriesArray = getSeriesArray();
        ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
        newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seriesSpinner.setAdapter(newSeriesAdapter);
        seriesSpinner.setSelection(seriesList.size() - 1);
    }

    // отдельный метод для добавления серии к графику
    private void addSeriesToPlot() {
        if (measurementUnitDB.equals("Точки и полуточки 2")) {
            pointAndDoublePoint();
        } else {
            if (seriesList.size() == 1 || seriesSpinner.getSelectedItemPosition() == 0) {
                plot.addSeries(curSeries, seriesFormat);
            } else {
                plot.addSeries(curSeries, seriesFormatPromer);
            }
        }
    }





    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            shiftPoint(1);
            handler.postDelayed(this, 50); // call plusPoint() every 50ms
        }
    };


    private Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            shiftPoint(-1);
            handler1.postDelayed(this, 50); // call plusPoint() every 50ms
        }
    };


    private void shiftPoint(int direction) {
        if (curElement == -1) {
            Toast.makeText(this, "Выберите значение для сдвига", Toast.LENGTH_SHORT).show();
        } else {
            int currentIndex = seriesList.indexOf(curSeries);
            count += direction;
            Number curX = Float.parseFloat(curSeries.getX(curElement).toString()) + direction;
            if (measurementUnitDB.equals("Точки")) {
                if (curElement >= 1 && (curSeries.size() - 1) - curElement >= 1) {
                    Number curX1 = Float.parseFloat(curSeries.getX(curElement + 1).toString()) - direction * 0.5;
                    curSeries.setX(curX1, curElement + 1);
                    Number curX2 = Float.parseFloat(curSeries.getX(curElement - 1).toString()) - direction * 0.5;
                    curSeries.setX(curX2, curElement - 1);
                } else {
                    if (curElement < 1 && (curSeries.size() - 1) - curElement >= 1) {
                        Number curX1 = Float.parseFloat(curSeries.getX(curElement + 1).toString()) - direction * 0.5;
                        curSeries.setX(curX1, curElement + 1);
                    }
                    if (curElement >= 1 && (curSeries.size() - 1) - curElement < 1) {
                        Number curX2 = Float.parseFloat(curSeries.getX(curElement - 1).toString()) - direction * 0.5;
                        curSeries.setX(curX2, curElement - 1);
                    }
                }
                curSeries.setX(curX, curElement);
            } else if (measurementUnitDB.equals("Точки и полуточки 1")) {
                if (curElement >= 2 && (curSeries.size() - 1) - curElement >= 2) {
                    Number curX1 = Float.parseFloat(curSeries.getX(curElement + 2).toString()) - direction * 0.5;
                    curSeries.setX(curX1, curElement + 2);
                    Number curX2 = Float.parseFloat(curSeries.getX(curElement - 2).toString()) - direction * 0.5;
                    curSeries.setX(curX2, curElement - 2);
                } else {
                    if (curElement < 2 && (curSeries.size() - 1) - curElement >= 2) {
                        Number curX1 = Float.parseFloat(curSeries.getX(curElement + 2).toString()) - direction * 0.5;
                        curSeries.setX(curX1, curElement + 2);
                    }
                    if (curElement >= 2 && (curSeries.size() - 1) - curElement < 2) {
                        Number curX2 = Float.parseFloat(curSeries.getX(curElement - 2).toString()) - direction * 0.5;
                        curSeries.setX(curX2, curElement - 2);
                    }
                }
                curSeries.setX(curX, curElement);
            } else if (measurementUnitDB.equals("Точки и полуточки 2")) {
                if (curElement >= 2 && (curSeries.size() - 1) - curElement >= 2) {
                    Number curX1 = Float.parseFloat(curSeries.getX(curElement + 2).toString()) - direction * 0.5;
                    curSeries.setX(curX1, curElement + 2);
                    Number curX2 = Float.parseFloat(curSeries.getX(curElement - 2).toString()) - direction * 0.5;
                    curSeries.setX(curX2, curElement - 2);
                } else {
                    if (curElement < 2 && (curSeries.size() - 1) - curElement >= 2) {
                        Number curX1 = Float.parseFloat(curSeries.getX(curElement + 2).toString()) - direction * 0.5;
                        curSeries.setX(curX1, curElement + 2);
                    }
                    if (curElement >= 2 && (curSeries.size() - 1) - curElement < 2) {
                        Number curX2 = Float.parseFloat(curSeries.getX(curElement - 2).toString()) - direction * 0.5;
                        curSeries.setX(curX2, curElement - 2);
                    }
                }
                curSeries.setX(curX, curElement);
                pointAndDoublePoint();
            }
            highlightedPoint.setX(curX, 0);
            List<SimpleXYSeries> subList = seriesList.subList(currentIndex + 1, seriesList.size());
            subList.clear();
            updateCountText();
            calculateDifference();
            countSeries = currentIndex;
            recyclerView.scrollToPosition(curElement);
        }
        if(count == 0){
            curElements.put(selectedSeriesIndex, -1);
            pointShiftMap.put(curElement, count);
        }else{
            curElements.put(selectedSeriesIndex, curElement);
            if(pointShiftMap.get(curElement)!=null){
                int currSdvig = pointShiftMap.get(curElement).intValue();
                pointShiftMap.put(curElement, currSdvig+direction);
            }else{
                pointShiftMap.put(curElement, count);
            }
        }

    }


    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etEnterVal.getWindowToken(), 0);
    }



    private void resetChangesList(){

        int currentIndex = seriesList.indexOf(curSeries);
        List<SimpleXYSeries> subList = seriesList.subList(currentIndex , seriesList.size());
        subList.clear();
        countSeries = currentIndex-1;
        createNewSeries(seriesList.get(countSeries));
        curSeries = seriesList.get(seriesList.size()-1);
        calculateDifference();
        String[] newSeriesArray = getSeriesArray();
        ArrayAdapter<String> newSeriesAdapter = new ArrayAdapter<>(ZamerActivity.this, android.R.layout.simple_spinner_item, newSeriesArray);
        newSeriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        seriesSpinner.setAdapter(newSeriesAdapter);
        seriesSpinner.setSelection(seriesList.size()-1);
        updateCountText();
        if(measurementUnitDB.equals("Точки и полуточки 2")){
            pointAndDoublePoint();
        }else{
            plot.clear();
            if(seriesList.size()==1 || seriesSpinner.getSelectedItemPosition()==0 ){
                plot.addSeries(curSeries, seriesFormat);
            }else{
                plot.addSeries(curSeries, seriesFormatPromer);
            }
        }
        plot.redraw();
    }


}
