package com.example.descosmartapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RechargeResponse {

    public int code;
    public String desc;

    /**
     * FIXED: API র data field "data" অথবা "rechargeList" হতে পারে
     */
    @SerializedName(value = "data", alternate = {"rechargeList", "list", "records", "history"})
    public List<RechargeRecord> data;

    public static class RechargeRecord {
        public String accountNo;
        public String meterNo;

        /**
         * FIXED: DESCO API বিভিন্ন field name ব্যবহার করে।
         * Gson এর alternate annotation দিয়ে সব possible নাম handle করা হয়েছে।
         * Website screenshot এ "Total Amount" column দেখা যাচ্ছে।
         */
        @SerializedName(value = "totalAmount", alternate = {
                "amount", "rechargeAmount", "taka", "rechargeTaka",
                "rechargeAmountBDT", "totalTaka"
        })
        public double amount;

        @SerializedName(value = "rechargeDate", alternate = {
                "rechargeTime", "transactionDate", "date",
                "rechargeDatetime", "rechargeDateTime", "txnDate"
        })
        public String rechargeDate;

        @SerializedName(value = "transactionId", alternate = {
                "txnId", "txnNo", "transId", "refNo",
                "referenceNo", "reference", "trxId"
        })
        public String transactionId;

        @SerializedName(value = "paymentMode", alternate = {
                "mode", "payMode", "paymentType", "channel",
                "paymentChannel", "medium"
        })
        public String paymentMode;

        @SerializedName(value = "energyAmount", alternate = {
                "energyUnit", "unit", "kwhAmount", "energy",
                "consumedUnit", "kwh", "energyKwh"
        })
        public double energyAmount;

        @SerializedName(value = "status", alternate = {
                "rechargeStatus", "txnStatus", "paymentStatus"
        })
        public String status;
    }
}