package com.example.descosmartapp.pattern.adapter;

import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.CustomerResponse;

/**
 * ADAPTER PATTERN — Converts raw API responses to unified UI-friendly model.
 * If API changes or a new provider is added, only this adapter needs updating.
 */
public class CustomerApiAdapter {

    public static class UnifiedCustomerModel {
        public String displayName;
        public String accountNo;
        public String meterNo;
        public String phone;
        public String address;
        public String feeder;
        public String phase;
        public double balance;
        public double monthlyUsage;
        public String lastReading;
        public String installDate;
        public String sdName;
    }

    public static UnifiedCustomerModel adapt(
            CustomerResponse.Customer customer,
            BalanceResponse.Balance balance) {

        UnifiedCustomerModel model = new UnifiedCustomerModel();

        if (customer != null) {
            model.displayName = safe(customer.customerName);
            model.accountNo   = safe(customer.accountNo);
            model.meterNo     = safe(customer.meterNo);
            model.phone       = safe(customer.contactNo);
            model.address     = safe(customer.installationAddress);
            model.feeder      = safe(customer.feederName);
            model.phase       = safe(customer.phaseType);
            model.installDate = safe(customer.installationDate);
            model.sdName      = safe(customer.SDName);
        }

        if (balance != null) {
            model.balance      = balance.balance;
            model.monthlyUsage = balance.currentMonthConsumption;
            model.lastReading  = safe(balance.readingTime);
        }

        return model;
    }

    private static String safe(String s) {
        return (s != null && !s.isEmpty()) ? s : "N/A";
    }
}