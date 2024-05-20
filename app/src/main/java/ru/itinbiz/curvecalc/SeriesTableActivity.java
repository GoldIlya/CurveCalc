package ru.itinbiz.curvecalc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.androidplot.xy.SimpleXYSeries;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ru.itinbiz.curvecalc.R;

public class SeriesTableActivity extends AppCompatActivity {
    private List<SimpleXYSeries> seriesList = new ArrayList<>();

    private TableLayout seriesTable;
    private TableLayout seriesTableInner;
    private ConstraintLayout constraintLayout;
    private Guideline guideLine;

    private float scaleFactor = 1.0f;
    private Matrix matrix = new Matrix();

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_table);

        // Get the seriesList data from the Intent

        Intent intent = getIntent();

        final String seriesListJson = (String) getIntent().getSerializableExtra("seriesListJson");
        Gson gson = new Gson();
        Type seriesListType = new TypeToken<ArrayList<SimpleXYSeries>>() {}.getType();
        seriesList = gson.fromJson(seriesListJson, seriesListType);

        // Initialize UI elements
        seriesTable = findViewById(R.id.series_table);
        seriesTableInner = findViewById(R.id.series_table_inner);
        constraintLayout = findViewById(R.id.constraint_layout);
        guideLine = findViewById(R.id.guide_line);

        // Create the table
        createTable(seriesTableInner, seriesList);

        // Initialize gesture detectors
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
                matrix.setScale(scaleFactor, scaleFactor);
                constraintLayout.setScaleX(scaleFactor);
                constraintLayout.setScaleY(scaleFactor);
                return true;
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                matrix.postTranslate(distanceX, distanceY);
                constraintLayout.setTranslationX(distanceX);
                constraintLayout.setTranslationY(distanceY);
                return true;
            }
        });

        // Set touch listener
        constraintLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    private void createTable(TableLayout tableLayout, List<SimpleXYSeries> seriesList) {
        // Add table headers
        TableRow headerRow = new TableRow(this);
        TextView xHeader = new TextView(this);
        xHeader.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
        xHeader.setPadding(2,0,2,0);
        xHeader.setText("Номер");
        headerRow.addView(xHeader);
        for (SimpleXYSeries series : seriesList) {
            TextView yHeader = new TextView(this);
            yHeader.setPadding(2,0,2,0);
            if(seriesList.indexOf(series)==0){
                yHeader.setText("Промер");
            }else{
                yHeader.setText("Шаг" + (seriesList.indexOf(series)));
            }
            headerRow.addView(yHeader);
        }
        tableLayout.addView(headerRow);

        // Add table rows for each data point in the seriesList
        int numRows = seriesList.get(0).size();
        for (int i = 0; i < numRows; i++) {
            TableRow dataRow = new TableRow(this);
            TextView yValue = new TextView(this);
            yValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
            yValue.setPadding(2,0,2,0);
            yValue.setText(String.valueOf(seriesList.get(0).getY(i)));
            dataRow.addView(yValue);
            for (SimpleXYSeries series : seriesList) {
                TextView xValue = new TextView(this);
                xValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
                xValue.setPadding(2,0,2,0);
                xValue.setText(String.valueOf(series.getX(i)));
                dataRow.addView(xValue);
            }
            tableLayout.addView(dataRow);
        }
    }
}