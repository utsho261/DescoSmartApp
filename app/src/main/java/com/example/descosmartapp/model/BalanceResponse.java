package com.example.descosmartapp.model;

public class BalanceResponse {

    public int code;
    public String desc;
    public Balance data;

    public static class Balance {
        public String accountNo;
        public String meterNo;
        public double balance;
        public double currentMonthConsumption;
        public String readingTime;
    }
}