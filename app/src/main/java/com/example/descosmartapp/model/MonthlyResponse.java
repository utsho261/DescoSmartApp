package com.example.descosmartapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MonthlyResponse {

    public int code;
    public String desc;
    public List<MonthlyRecord> data;

    public static class MonthlyRecord {
        public String accountNo;

        @SerializedName("monthYear")
        public String month;

        @SerializedName("consumedUnit")
        public double consumption;

        @SerializedName("consumedTaka")
        public double amount;
    }
}