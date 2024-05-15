package ru.itinbiz.curvecalc.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "measurement_table")
public class Measurement {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String measurementUnit;
    private int countPoint;
    private int countSeries;
    private String seriesListJson; // Save the seriesList as a JSON string

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

    public int getCountPoint() {
        return countPoint;
    }

    public void setCountPoint(int countPoint) {
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
}