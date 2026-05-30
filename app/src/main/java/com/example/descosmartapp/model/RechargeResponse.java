package com.example.descosmartapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RechargeResponse {

    public int code;
    public String desc;
    public List<RechargeRecord> data;

    public static class RechargeRecord {
        public String accountNo;
        public String meterNo;

        @SerializedName("totalAmount")
        public double amount;

        @SerializedName("rechargeDate")
        public String rechargeDate;

        @SerializedName("transactionId")
        public String transactionId;

        @SerializedName("paymentMode")
        public String paymentMode;

        @SerializedName("energyAmount")
        public double energyAmount;
    }
}