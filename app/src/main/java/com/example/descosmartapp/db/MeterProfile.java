package com.example.descosmartapp.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "meters")
public class MeterProfile {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String label;        // "Home", "Office", custom name
    public String accountNo;
    public String meterNo;
    public double lowBalanceThreshold; // alert below this
    public boolean isActive;    // currently selected
    public long createdAt;

    // State pattern field
    public String meterState;   // "ACTIVE", "INACTIVE", "SUSPENDED"
}