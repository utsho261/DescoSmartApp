package com.example.descosmartapp.api;

import com.example.descosmartapp.model.BalanceResponse;
import com.example.descosmartapp.model.CustomerResponse;
import com.example.descosmartapp.model.MonthlyResponse;
import com.example.descosmartapp.model.RechargeResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DescoApi {

    @GET("getCustomerInfo")
    Call<CustomerResponse> getCustomerInfo(@Query("accountNo") String accountNo);

    @GET("getCustomerInfo")
    Call<CustomerResponse> getCustomerByMeter(@Query("meterNo") String meterNo);

    @GET("getBalance")
    Call<BalanceResponse> getBalance(@Query("accountNo") String accountNo);

    /** Monthly — YYYYMM format */
    @GET("getCustomerMonthlyConsumption")
    Call<MonthlyResponse> getMonthlyConsumption(
            @Query("accountNo") String accountNo,
            @Query("monthFrom") String from,
            @Query("monthTo") String to);

    /** Recharge history — accountNo দিয়ে */
    @GET("getRechargeHistory")
    Call<RechargeResponse> getRechargeHistory(
            @Query("accountNo") String accountNo,
            @Query("dateFrom") String from,
            @Query("dateTo") String to);

    /**
     * ADDED: Recharge history — meterNo দিয়ে (fallback)
     * কিছু DESCO API endpoint meterNo accept করে accountNo এর বদলে
     */
    @GET("getRechargeHistory")
    Call<RechargeResponse> getRechargeHistoryByMeter(
            @Query("meterNo") String meterNo,
            @Query("dateFrom") String from,
            @Query("dateTo") String to);
}