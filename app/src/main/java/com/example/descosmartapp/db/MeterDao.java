package com.example.descosmartapp.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MeterDao {

    @Insert
    long insert(MeterProfile meter);

    @Update
    void update(MeterProfile meter);

    @Delete
    void delete(MeterProfile meter);

    @Query("SELECT * FROM meters ORDER BY createdAt ASC")
    List<MeterProfile> getAllMeters();

    @Query("SELECT * FROM meters WHERE isActive = 1 LIMIT 1")
    MeterProfile getActiveMeter();

    @Query("UPDATE meters SET isActive = 0")
    void deactivateAll();

    @Query("UPDATE meters SET isActive = 1 WHERE id = :id")
    void setActive(int id);

    @Query("SELECT COUNT(*) FROM meters")
    int getCount();
}