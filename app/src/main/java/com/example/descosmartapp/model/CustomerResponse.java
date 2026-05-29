package com.example.descosmartapp.model;

public class CustomerResponse {

    public int code;
    public String desc;
    public Customer data;

    public static class Customer {
        public String accountNo;
        public String meterNo;
        public String customerName;
        public String contactNo;
        public String installationAddress;
        public String feederName;
        public String phaseType;
        public String installationDate;
        public String SDName;
    }
}