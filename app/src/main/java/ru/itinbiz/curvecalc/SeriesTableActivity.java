package ru.itinbiz.curvecalc;

import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.androidplot.xy.SimpleXYSeries;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SeriesTableActivity extends AppCompatActivity {
    private List<SimpleXYSeries> seriesList = new ArrayList<>();
    

    private TableLayout seriesTable;


    private float scaleFactor = 1.0f;
    private Matrix matrix = new Matrix();

    private ScaleGestureDetector scaleGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_table);

        // Get the seriesList data from the Intent
        Intent intent = getIntent();
        String seriesListJson = intent.getStringExtra("seriesListJson");
        Gson gson = new Gson();
        Type seriesListType = new TypeToken<ArrayList<SimpleXYSeries>>() {}.getType();
        seriesList = gson.fromJson(seriesListJson, seriesListType);

        // Initialize UI elements
        seriesTable = findViewById(R.id.series_table);
        ConstraintLayout seriesTableContainer = findViewById(R.id.series_table_container);
        seriesTableContainer.removeView(seriesTable);

        // Wrap the TableLayout in a HorizontalScrollView and a ScrollView
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        ScrollView scrollView = new ScrollView(this);
        horizontalScrollView.addView(seriesTable);
        scrollView.addView(horizontalScrollView);



        // Add the ScrollView to the layout
        ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout);
        constraintLayout.addView(scrollView);

        // Create the table
        createTable(seriesTable, seriesList);

        // Initialize gesture detectors
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
                matrix.setScale(scaleFactor, scaleFactor);
                seriesTable.setScaleX(scaleFactor);
                seriesTable.setScaleY(scaleFactor);

                // Adjust the scrolling behavior based on the zoom level
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_UP);
                    }
                });


                return true;
            }
        });

        // Set touch listener
        seriesTable.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
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
            boolean isInteger = (seriesList.get(0).getY(i).floatValue() - Math.floor(seriesList.get(0).getY(i).floatValue())) == 0;
            if(isInteger){
                yValue.setText(String.valueOf(seriesList.get(0).getY(i)));
            }else{
                yValue.setText(" - ");
            }
            dataRow.addView(yValue);
            for (SimpleXYSeries series : seriesList) {
                int indexSeries = seriesList.indexOf(series);
                TextView xValue = new TextView(this);
                xValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
                xValue.setPadding(2,0,2,0);
                if(indexSeries>0){
                    SimpleXYSeries diff =  calculateDifference(seriesList.get(indexSeries-1), series, indexSeries);
                    Double iDiff = diff.getX(i).doubleValue();
                    if (iDiff != 0.0){
                        if(iDiff>0){
                            xValue.setText(String.valueOf(series.getX(i))+"  "+"+"+iDiff);
                        }else {
                            xValue.setText(String.valueOf(series.getX(i))+"  "+iDiff);
                        }
                        xValue.setTextColor(this.getResources().getColor(R.color.green));
                    }else{
                        xValue.setText(String.valueOf(series.getX(i)));
                    }
                }
                else{
                    xValue.setText(String.valueOf(series.getX(i)));
                }
                dataRow.addView(xValue);
            }
            tableLayout.addView(dataRow);
        }
    }
    private SimpleXYSeries calculateDifference(SimpleXYSeries prevSeries, SimpleXYSeries curSeries, int indexCurSeries) {
        SimpleXYSeries differenceSeries = new SimpleXYSeries("Difference");

        if (seriesList.size() > 0 && indexCurSeries > 0 ) {
            // Get the two selected series from the spinner
            SimpleXYSeries series1 = prevSeries;
            SimpleXYSeries series2 = curSeries;
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

}