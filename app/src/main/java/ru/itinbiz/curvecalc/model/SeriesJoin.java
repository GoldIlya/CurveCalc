package ru.itinbiz.curvecalc.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "series_join_table")
public class SeriesJoin {

    @PrimaryKey
    private int id;

    @ColumnInfo(name = "measurement_id")
    private int measurementId;

    @ColumnInfo(name = "simple_xy_series_id")
    private int simpleXYSeriesId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId(int measurementId) {
        this.measurementId = measurementId;
    }

    public int getSimpleXYSeriesId() {
        return simpleXYSeriesId;
    }

    public void setSimpleXYSeriesId(int simpleXYSeriesId) {
        this.simpleXYSeriesId = simpleXYSeriesId;
    }
}