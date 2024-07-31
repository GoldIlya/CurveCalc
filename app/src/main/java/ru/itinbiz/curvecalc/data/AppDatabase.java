package ru.itinbiz.curvecalc.data;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ru.itinbiz.curvecalc.model.Measurement;

@Database(entities = {Measurement.class}, version = 6, autoMigrations = {
        @AutoMigration (from = 4, to = 5),
        @AutoMigration (from = 5, to = 6)} ,  exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract MeasurementDao measurementDao();

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measurement_table ADD COLUMN pointShiftJson TEXT");

        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add your migration logic here, e.g., create new tables, alter existing ones, etc.
            // For example, let's assume you added a new column to the measurement_table
            database.execSQL("ALTER TABLE measurement_table ADD COLUMN curElementsJson TEXT");
        }
    };
}

