package ru.itinbiz.curvecalc.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "measurement_table")
public class Measurement {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String measurementUnit;
    private double countPoint;
    private int countSeries;
    private String seriesListJson; // Save the seriesList as a JSON string
    private String baseMeasurJson;
    private String curElementsJson; // Save the curElements as a JSON string
    @ColumnInfo(name = "pointShiftJson")
    private String pointShiftJson;

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {return name;}

    public void setName(String name) {this.name = name;}

    public String getMeasurementUnit() {return measurementUnit;}

    public void setMeasurementUnit(String measurementUnit) {this.measurementUnit = measurementUnit;}

    public double getCountPoint() {
        return countPoint;
    }

    public void setCountPoint(double countPoint) {
        this.countPoint = countPoint;
    }

    public int getCountSeries() {
        return countSeries;
    }

    public void setCountSeries(int countSeries) {
        this.countSeries = countSeries;
    }

    public String getSeriesListJson() {
        return seriesListJson;
    }

    public void setSeriesListJson(String seriesListJson) {
        this.seriesListJson = seriesListJson;
    }

    public String getBaseMeasurJson() {return baseMeasurJson;}

    public void setBaseMeasurJson(String baseMeasurJson) {this.baseMeasurJson = baseMeasurJson;}

    public String getCurElementsJson() {return curElementsJson;}

    public void setCurElementsJson(String curElementsJson) {this.curElementsJson = curElementsJson;}

    public String getPointShiftJson() {return pointShiftJson;}

    public void setPointShiftJson(String pointShiftJson) {this.pointShiftJson = pointShiftJson;}
}