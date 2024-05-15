package ru.itinbiz.curvecalc.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ru.itinbiz.curvecalc.model.Measurement;

@Dao
public interface MeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Measurement measurement);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertAndReturnId(Measurement measurement);

    @Query("SELECT * FROM measurement_table ORDER BY id DESC LIMIT 1")
    LiveData<Measurement> getLastMeasurement();

    @Query("SELECT * FROM measurement_table WHERE id = :id")
    Measurement getMeasurementById(long id);

    @Query("DELETE FROM measurement_table")
    void deleteAllMeasurements();

    @Query("DELETE FROM measurement_table WHERE id = :id")
    void deleteMeasurementById(long id);

    // Add this method to update a measurement
    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateMeasurement(Measurement measurement);

    // Add this method to get all measurements
    @Query("SELECT * FROM measurement_table ORDER BY id DESC")
    List<Measurement> getAllMeasurements();
}