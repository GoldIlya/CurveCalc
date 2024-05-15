package ru.itinbiz.curvecalc.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.androidplot.xy.SimpleXYSeries;

import java.util.List;

public class MeasurementWithSeries {

    @Embedded
    private Measurement measurement;

    @Relation(
            parentColumn = "id",
            entityColumn = "measurementId",
            associateBy = @Junction(SeriesJoin.class)
    )
    private List<SimpleXYSeries> seriesList;


}