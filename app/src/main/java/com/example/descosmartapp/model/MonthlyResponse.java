package com.example.descosmartapp.model;

import java.util.List;

public class MonthlyResponse {

    public int code;
    public String desc;
    public List<MonthlyRecord> data;

    public static class MonthlyRecord {
        public String accountNo;
        public String month;
        public double consumption;
        public double amount;
    }
}