package ru.itinbiz.curvecalc;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.androidplot.xy.SimpleXYSeries;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeriesTableActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_CODE_CREATE_DOCUMENT = 100;
    private static final int REQUEST_CODE_SAVE_PDF_DOCUMENT = 101;

    private static final int REQUEST_CODE_CREATE_PDF_DOCUMENT = 200 ;
    private static final int REQUEST_CODE_SAVE_JSON = 202;


    private List<SimpleXYSeries> seriesList = new ArrayList<>();
    private Map<Integer, Integer> curElements = new HashMap<>();
    private Map<Integer, Integer> pointShiftSum = new HashMap<>();
    private Map<Integer, Map<Integer, Integer>> pointShiftMap = new HashMap<>();
    private String nameZamer, measurementUnit;
    private TableLayout seriesTable;
    private float scaleFactor = 1.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private FrameLayout frameLayout;
    private HorizontalScrollView horizontalScrollView;
    private ScrollView scrollView;
    private Button btnExcel;
    Workbook workbook = new HSSFWorkbook();
    Sheet sheet = workbook.createSheet("Sheet1");
    PdfDocument document = new PdfDocument();
    Bitmap bitmap, bitmap1;
    Uri targetPdf;
    boolean boolean_save;
    boolean boolean_permission;
    private Double countPoint;
    private int countSeries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_table);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get the seriesList data from the Intent
        Intent intent = getIntent();
        String seriesListJson = intent.getStringExtra("seriesListJson");
        String curElementsToJson = intent.getStringExtra("curElementsToJson");
        String pointShiftJson = intent.getStringExtra("pointShiftJson");
        String pointShiftSumJson = intent.getStringExtra("pointShiftSumJson");
        Gson gson = new Gson();
        Type seriesListType = new TypeToken<ArrayList<SimpleXYSeries>>() {}.getType();
        seriesList = gson.fromJson(seriesListJson, seriesListType);
        Type curElementsType = new TypeToken<Map<Integer, Integer>>() {}.getType();
        curElements = gson.fromJson(curElementsToJson, curElementsType);

        Type typeShift = new TypeToken<Map<Integer, Map<Integer, Integer>>>() {}.getType();
        pointShiftMap = gson.fromJson(pointShiftJson, typeShift);

        Type typeShiftSum = new TypeToken<Map<Integer, Integer>>() {}.getType();
        pointShiftSum = gson.fromJson(pointShiftSumJson, typeShiftSum);

        nameZamer = (String) getIntent().getSerializableExtra("nameZamer");
        measurementUnit = (String) getIntent().getSerializableExtra("measurementUnit");
        countPoint = (Double) getIntent().getSerializableExtra("countPointLF");
        countSeries = (int) getIntent().getSerializableExtra("countSeriesLF");
//        btnExcel = findViewById(R.id.btnExcel);
        fn_permission();
        // Initialize UI elements

        seriesTable = new TableLayout(this);

        // Wrap the TableLayout in a HorizontalScrollView and a ScrollView
        horizontalScrollView = new HorizontalScrollView(this);
        scrollView = new ScrollView(this);
        frameLayout = new FrameLayout(this);

//        updatePaddingAndGravity();

        frameLayout.addView(seriesTable);
        horizontalScrollView.addView(frameLayout);
        scrollView.addView(horizontalScrollView);

        // Add the ScrollView to the layout
        ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout);
        constraintLayout.addView(scrollView);

        // Create the table
        createTable(seriesTable, seriesList, pointShiftSum);

//        btnExcel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (boolean_permission) {
//                    Toast.makeText(SeriesTableActivity.this, "Excell", Toast.LENGTH_SHORT).show();
//                    createAndSaveExcelTable();
//                    openPath();
//                }
//            }
//        });

        // Initialize gesture detectors
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

                // Set the pivot points to the current touch location
                frameLayout.setPivotX(detector.getFocusX());
                frameLayout.setPivotY(detector.getFocusY());

                frameLayout.setScaleX(scaleFactor);
                frameLayout.setScaleY(scaleFactor);

                updatePaddingAndGravity();

                // Adjust the scrolling behavior based on the zoom level
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.scrollTo(0, scrollView.getBottom());
                    }
                });
                horizontalScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        horizontalScrollView.scrollTo(horizontalScrollView.getRight(), 0);
                    }
                });

                return true;
            }
        });

        // Set touch listener for scaling and scrolling
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return false; // Return false to allow both scrolling and zooming
            }
        };

        frameLayout.setOnTouchListener(touchListener);
        horizontalScrollView.setOnTouchListener(touchListener);
        scrollView.setOnTouchListener(touchListener);


    }



    private void updatePaddingAndGravity() {
        int paddingInDp = 100;
        float basePaddingInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                paddingInDp,
                getResources().getDisplayMetrics()
        );
        int scaledPadding = (int) (basePaddingInPx * scaleFactor*2);
        seriesTable.setPadding(scaledPadding, scaledPadding, scaledPadding, scaledPadding);

        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        seriesTable.setForegroundGravity(Gravity.CENTER);
        seriesTable.requestLayout();
    }

    private void createTable(TableLayout tableLayout, List<SimpleXYSeries> seriesList, Map<Integer, Integer> pointIndexSums) {
        tableLayout.removeAllViews();
        // Add table headers
        TableRow headerRow = new TableRow(this);
        TextView xHeader = new TextView(this);
        xHeader.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
        xHeader.setPadding(2, 0, 2, 0);
        xHeader.setText("Номер");
        headerRow.addView(xHeader);

        TextView movementHeader = new TextView(this);
        movementHeader.setPadding(2, 0, 2, 0);
        movementHeader.setText("Движение");
        headerRow.addView(movementHeader);

        for (SimpleXYSeries series : seriesList) {
            TextView yHeader = new TextView(this);
            yHeader.setPadding(2, 0, 2, 0);
            if (seriesList.indexOf(series) == 0) {
                yHeader.setText("Промер");
            } else {
                yHeader.setText("Шаг " + (seriesList.indexOf(series)));
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
            yValue.setPadding(2, 0, 2, 0);
            boolean isInteger = (seriesList.get(0).getY(i).floatValue() - Math.floor(seriesList.get(0).getY(i).floatValue())) == 0;
            if (isInteger) {
                yValue.setText(String.valueOf(Math.round(seriesList.get(0).getY(i).floatValue())));
            } else {
                yValue.setText(" - ");
            }
            dataRow.addView(yValue);

            TextView movementValue = new TextView(this);
            movementValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
            movementValue.setPadding(2, 0, 2, 0);
            Integer movement = pointIndexSums.get(i);
            if (movement == null) {
                movementValue.setText("0.0");
            } else {
                if(movement > 0){
                    movementValue.setText("  +"+movement.intValue());
                }else{
                    movementValue.setText("  "+movement.intValue());
                }
                movementValue.setTextColor(this.getResources().getColor(R.color.red));
            }
            dataRow.addView(movementValue);

            for (SimpleXYSeries series : seriesList) {
                int indexSeries = seriesList.indexOf(series);
                TextView xValue = new TextView(this);
                xValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
                xValue.setPadding(2, 0, 2, 0);
                if (indexSeries > 0) {
                    SimpleXYSeries diff = calculateDifference(seriesList.get(indexSeries - 1), series, indexSeries);
                    Double iDiff = diff.getX(i).doubleValue();
                    if (iDiff != 0.0) {
                        if (iDiff > 0) {
                            xValue.setText(String.valueOf(series.getX(i)) + "  " + "+" + iDiff);
                        } else {
                            xValue.setText(String.valueOf(series.getX(i)) + "  " + iDiff);
                        }
                        if(isInteger){
                            if(curElements != null){
                                Integer value = curElements.get(indexSeries);
                                if(value != null && value.intValue() == i){
                                    xValue.setTextColor(this.getResources().getColor(R.color.red));
                                }else{
                                    xValue.setTextColor(this.getResources().getColor(R.color.green));
                                }
                            }else{
                                // handle the case where curElements is null
                            }
                        }else{
                            if(curElements != null){
                                Integer value = curElements.get(indexSeries);
                                if(value != null && value.intValue() == i){
                                    xValue.setTextColor(this.getResources().getColor(R.color.red));
                                }else{
                                    xValue.setTextColor(this.getResources().getColor(R.color.blue));
                                }
                            }else{
                                // handle the case where curElements is null
                            }
                        }

                    } else {
                        xValue.setText(String.valueOf(series.getX(i)));
                    }
                } else {
                    xValue.setText(String.valueOf(series.getX(i)));
                }
                dataRow.addView(xValue);
            }
            tableLayout.addView(dataRow);
            tableLayout.setPadding(100,100,100,100);
        }
    }



    private void createPivotTable(TableLayout tableLayout, List<SimpleXYSeries> seriesList, Map<Integer, Integer> pointShiftMap) {
        tableLayout.removeAllViews();
        // Add table headers
        TableRow headerRow = new TableRow(this);
        TextView xHeader = new TextView(this);
        xHeader.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
        xHeader.setPadding(2, 0, 2, 0);
        xHeader.setText("№");
        headerRow.addView(xHeader);
        TextView pointShiftHeader = new TextView(this);
        pointShiftHeader.setPadding(2, 0, 2, 0);
        pointShiftHeader.setText("Движение");
        headerRow.addView(pointShiftHeader);
        for (SimpleXYSeries series : seriesList) {
            TextView yHeader = new TextView(this);
            yHeader.setPadding(2, 0, 2, 0);
            if (seriesList.indexOf(series) == 0) {
                yHeader.setText("Промер");
            } else {
                yHeader.setText("Шаг " + (seriesList.indexOf(series)));
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
            yValue.setPadding(2, 0, 2, 0);
            boolean isInteger = (seriesList.get(0).getY(i).floatValue() - Math.floor(seriesList.get(0).getY(i).floatValue())) == 0;
            if (isInteger) {
                yValue.setText(String.valueOf(Math.round(seriesList.get(0).getY(i).floatValue())));
            } else {
                yValue.setText(" - ");
            }
            dataRow.addView(yValue);
            TextView pointShiftValue = new TextView(this);
            pointShiftValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
            pointShiftValue.setPadding(2, 0, 2, 0);
            Integer pointShift = pointShiftMap.get(i);
            if (pointShift!= null) {
                pointShiftValue.setText(String.valueOf(pointShift));
            } else {
                pointShiftValue.setText("0");
            }
            dataRow.addView(pointShiftValue);
            for (SimpleXYSeries series : seriesList) {
                TextView xValue = new TextView(this);
                xValue.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
                xValue.setPadding(2, 0, 2, 0);
                xValue.setText(String.valueOf(series.getX(i)));
                dataRow.addView(xValue);
            }
            tableLayout.addView(dataRow);
            tableLayout.setPadding(100,100,100,100);
        }
    }



    private SimpleXYSeries calculateDifference(SimpleXYSeries prevSeries, SimpleXYSeries curSeries, int indexCurSeries) {
        SimpleXYSeries differenceSeries = new SimpleXYSeries("Difference");

        if (seriesList.size() > 0 && indexCurSeries > 0) {
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
        }
        return differenceSeries;
    }

    private void fn_permission() {
        if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)||
                (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {


            if ((ActivityCompat.shouldShowRequestPermissionRationale(SeriesTableActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE))) {
            } else {
                ActivityCompat.requestPermissions(SeriesTableActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);

            }

            if ((ActivityCompat.shouldShowRequestPermissionRationale(SeriesTableActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            } else {
                ActivityCompat.requestPermissions(SeriesTableActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);

            }
        } else {
            boolean_permission = true;

        }
    }


    private void createAndSaveExcelTable() {
        Row headerRow = sheet.createRow(0);
        Cell xHeader = headerRow.createCell(0);
        xHeader.setCellValue("Номер");
        Cell movementHeader = headerRow.createCell(1);
        movementHeader.setCellValue("Движение");
        for (SimpleXYSeries series : seriesList) {
            Cell yHeader = headerRow.createCell(seriesList.indexOf(series) + 2);
            if (seriesList.indexOf(series) == 0) {
                yHeader.setCellValue("Промер");
            } else {
                yHeader.setCellValue("Шаг " + seriesList.indexOf(series));
            }
        }

        // Add table rows for each data point in the seriesList
        int numRows = seriesList.get(0).size();
        for (int i = 0; i < numRows; i++) {
            Row dataRow = sheet.createRow(i + 1);
            Cell yValue = dataRow.createCell(0);
            boolean isInteger = (seriesList.get(0).getY(i).doubleValue() - Math.floor(seriesList.get(0).getY(i).doubleValue())) == 0;
            if (isInteger) {
                yValue.setCellValue(seriesList.get(0).getY(i).doubleValue());
            } else {
                yValue.setCellValue(" - ");
            }

            Cell movementValue = dataRow.createCell(1);
            Integer movement = pointShiftSum.get(i);
            if (movement == null) {
                movementValue.setCellValue(0.0);
            } else {
                if (movement > 0) {
                    movementValue.setCellValue("+" + movement);
                } else {
                    movementValue.setCellValue(movement);
                }
            }

            for (SimpleXYSeries series : seriesList) {
                int indexSeries = seriesList.indexOf(series);
                Cell xValue = dataRow.createCell(indexSeries + 2);
                if (indexSeries > 0) {
                    SimpleXYSeries diff = calculateDifference(seriesList.get(indexSeries - 1), series, indexSeries);
                    Double iDiff = diff.getX(i).doubleValue();
                    if (iDiff != 0.0) {
                        if (iDiff > 0) {
                            xValue.setCellValue(series.getX(i).doubleValue() + "  +" + iDiff);
                        } else {
                            xValue.setCellValue(series.getX(i).doubleValue() + "  " + iDiff);
                        }

                        // Set the cell style for the x-value cell

                        if (isInteger) {

                        } else {

                        }

                    } else {
                        xValue.setCellValue(series.getX(i).doubleValue());
                    }
                } else {
                    xValue.setCellValue(series.getX(i).doubleValue());
                }
            }
        }
    }




    private void openPath() {
        // write the document content
        String fileName = nameZamer+".xls";
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String mimeType = mimeTypeMap.getMimeTypeFromExtension("xls");
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                     FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
                    // Write the Excel document content to the file
                    workbook.write(fos);
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_CODE_SAVE_PDF_DOCUMENT && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                     FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
                    // Write the PDF document content to the file
                    document.writeTo(fos);
                    document.close();
                    targetPdf = uri;
                    boolean_save = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    boolean_save = false;
                    document.close();
                }
            }
        } else if (requestCode == REQUEST_CODE_CREATE_PDF_DOCUMENT && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                     FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
                    // Write the PDF document content to the file
                    document.writeTo(fos);
                    document.close();
                    targetPdf = uri;
                    boolean_save = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    boolean_save = false;
                    document.close();
                }
            }
        } else if (requestCode == REQUEST_CODE_SAVE_JSON && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                     FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {

                    // Получаем данные из Intent
                    String seriesListJson = getIntent().getStringExtra("seriesListJson");
                    String curElementsToJson = getIntent().getStringExtra("curElementsToJson");
                    String pointShiftJson = getIntent().getStringExtra("pointShiftJson");
                    String measurementUnit = getIntent().getStringExtra("measurementUnit");
                    Double countPointLF = countPoint;
                    int countSeriesLF = countSeries;
                    String nameZamerLF = nameZamer;

                    // Создаем JSON объект для объединения всех данных
                    Gson gson = new Gson();
                    Map<String, String> dataMap = new HashMap<>();
                    dataMap.put("seriesListJson", seriesListJson);
                    dataMap.put("curElementsToJson", curElementsToJson);
                    dataMap.put("pointShiftJson", pointShiftJson);
                    dataMap.put("measurementUnit", measurementUnit);
                    dataMap.put("countPointLF", String.valueOf(countPointLF));
                    dataMap.put("countSeriesLF", String.valueOf(countSeriesLF));
                    dataMap.put("nameZamerLF", String.valueOf(nameZamerLF));

                    String combinedJson = gson.toJson(dataMap);

                    // Записываем данные в файл
                    fos.write(combinedJson.getBytes());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_series_table, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_excel) {
            // Handle the Excel button click
            createAndSaveExcelTable();
            openPath();
            return true;
        }
        if (id == R.id.action_pdf) {
            createPdfAndSave();
            return true;
        }if (id == R.id.action_json) {
            saveDataToJsonFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private PdfDocument createPdf(){

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        float hight = seriesTable.getHeight();
        float width = seriesTable.getWidth();
        int convertHighet = (int) hight, convertWidth = (int) width;
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(seriesTable.getWidth(), seriesTable.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        canvas.drawPaint(paint);
        bitmap = Bitmap.createScaledBitmap(bitmap, seriesTable.getWidth(), seriesTable.getHeight(), true);
        paint.setColor(Color.WHITE);
        canvas.drawARGB(255, 255, 255, 255);
        canvas.drawBitmap(bitmap, 50, 50, paint);
        document.finishPage(page);
        int height;
        System.out.println("Документ"+document.getPages().toString());
        return document;
    }


    private void createPdfAndSave() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        float hight = displaymetrics.heightPixels;
        float width = displaymetrics.widthPixels;
        bitmap = loadBitmapFromView(seriesTable, seriesTable.getWidth(), seriesTable.getHeight());
        document = createPdf();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, nameZamer + ".pdf");
        startActivityForResult(intent, REQUEST_CODE_CREATE_PDF_DOCUMENT);
    }

    public static Bitmap loadBitmapFromView(View v, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }


    private void saveDataToJsonFile() {
        // Получаем данные из Intent
        String seriesListJson = getIntent().getStringExtra("seriesListJson");
        String curElementsToJson = getIntent().getStringExtra("curElementsToJson");
        String pointShiftJson = getIntent().getStringExtra("pointShiftJson");
        String measurementUnit = getIntent().getStringExtra("measurementUnit");
        String nameZamerLF = nameZamer;
        Double countPointLF = countPoint;
        int countSeriesLF = countSeries;



        // Создаем JSON объект для объединения всех данных
        Gson gson = new Gson();
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("seriesListJson", seriesListJson);
        dataMap.put("curElementsToJson", curElementsToJson);
        dataMap.put("pointShiftJson", pointShiftJson);
        dataMap.put("measurementUnit", measurementUnit);
        dataMap.put("countPointLF", String.valueOf(countPointLF));
        dataMap.put("countSeriesLF", String.valueOf(countSeriesLF));
        dataMap.put("nameZamerLF", String.valueOf(nameZamerLF));

        String combinedJson = gson.toJson(dataMap);

        // Создаем Intent для сохранения файла
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, nameZamer +".json");

        // Запускаем Intent для сохранения файла
        startActivityForResult(intent, REQUEST_CODE_SAVE_JSON);
    }

}