package com.example.descosmartapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.descosmartapp.utils.Constants;

public class ApiClient {

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}