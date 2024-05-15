package ru.itinbiz.curvecalc.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.github.mikephil.charting.data.Entry;

import java.util.List;

@Entity(tableName = "simple_xy_series_table")
public class EntitySimpleXYSeries {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private List<Entry> entries;

    public int getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}