package com.example.descosmartapp.model;

import java.util.List;

public class RechargeResponse {

    public int code;
    public String desc;
    public List<RechargeRecord> data;

    public static class RechargeRecord {
        public String accountNo;
        public String meterNo;
        public double amount;
        public String rechargeDate;
        public String transactionId;
        public String paymentMode;
    }
}