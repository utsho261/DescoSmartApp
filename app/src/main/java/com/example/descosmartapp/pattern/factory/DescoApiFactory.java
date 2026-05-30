package com.example.descosmartapp.pattern.factory;

import com.example.descosmartapp.api.ApiClient;
import com.example.descosmartapp.api.DescoApi;
import com.example.descosmartapp.utils.Constants;

public class DescoApiFactory extends ElectricityApiFactory {

    @Override
    public DescoApi createApi() {
        return ApiClient.getClient().create(DescoApi.class);
    }

    @Override
    public String getBaseUrl() {
        return Constants.BASE_URL;
    }
}