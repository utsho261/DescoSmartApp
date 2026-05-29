package com.example.descosmartapp.repository;

import com.example.descosmartapp.api.ApiClient;
import com.example.descosmartapp.api.DescoApi;
import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.CustomerResponse;

import retrofit2.Callback;

public class DescoRepository {

    private DescoApi api;

    public DescoRepository() {
        api = ApiClient.getClient().create(DescoApi.class);
    }

    public void getCustomerInfo(String accountNo, Callback<CustomerResponse> callback) {
        api.getCustomerInfo(accountNo).enqueue(callback);
    }

    public void getBalance(String accountNo, Callback<BalanceResponse> callback) {
        api.getBalance(accountNo).enqueue(callback);
    }
}