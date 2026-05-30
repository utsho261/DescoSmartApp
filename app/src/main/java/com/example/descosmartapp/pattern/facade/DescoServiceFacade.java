package com.example.descosmartapp.pattern.facade;

import com.example.descosmartapp.api.ApiClient;
import com.example.descosmartapp.api.DescoApi;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.CustomerResponse;
import com.example.descosmartapp.model.MonthlyResponse;
import com.example.descosmartapp.model.RechargeResponse;

import retrofit2.Callback;

public class DescoServiceFacade {

    private static DescoServiceFacade instance;
    private final DescoApi api;

    private DescoServiceFacade() {
        api = ApiClient.getClient().create(DescoApi.class);
    }

    public static synchronized DescoServiceFacade getInstance() {
        if (instance == null) instance = new DescoServiceFacade();
        return instance;
    }

    public void fetchCustomerByAccount(String accountNo, Callback<CustomerResponse> cb) {
        api.getCustomerInfo(accountNo).enqueue(cb);
    }

    public void fetchCustomerByMeter(String meterNo, Callback<CustomerResponse> cb) {
        api.getCustomerByMeter(meterNo).enqueue(cb);
    }

    public void fetchBalance(String accountNo, Callback<BalanceResponse> cb) {
        api.getBalance(accountNo).enqueue(cb);
    }

    public void fetchMonthly(String accountNo, String from, String to, Callback<MonthlyResponse> cb) {
        api.getMonthlyConsumption(accountNo, from, to).enqueue(cb);
    }

    /** accountNo দিয়ে recharge history */
    public void fetchRechargeHistory(String accountNo, String from, String to, Callback<RechargeResponse> cb) {
        api.getRechargeHistory(accountNo, from, to).enqueue(cb);
    }

    /** meterNo দিয়ে recharge history (fallback) */
    public void fetchRechargeHistoryByMeter(String meterNo, String from, String to, Callback<RechargeResponse> cb) {
        api.getRechargeHistoryByMeter(meterNo, from, to).enqueue(cb);
    }

    public void smartFetch(String input, Callback<CustomerResponse> cb) {
        if (input.length() > 10) {
            fetchCustomerByMeter(input, cb);
        } else {
            fetchCustomerByAccount(input, cb);
        }
    }
}