package com.example.descosmartapp.pattern.factory;

import com.example.descosmartapp.api.DescoApi;
import com.example.descosmartapp.api.ApiClient;

/**
 * ABSTRACT FACTORY PATTERN
 * Different electricity providers can be added later (DPDC, NESCO, etc.)
 */
public abstract class ElectricityApiFactory {

    public abstract Object createApi();
    public abstract String getBaseUrl();

    // Factory method — returns correct factory by provider name
    public static ElectricityApiFactory getFactory(String provider) {
        switch (provider.toUpperCase()) {
            case "DESCO": return new DescoApiFactory();
            // case "DPDC": return new DpdcApiFactory();
            default: return new DescoApiFactory();
        }
    }
}