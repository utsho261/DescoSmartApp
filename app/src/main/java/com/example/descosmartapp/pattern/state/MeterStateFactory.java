package com.example.descosmartapp.pattern.state;

public class MeterStateFactory {
    public static MeterState fromString(String state) {
        if (state == null) return new ActiveMeterState();
        switch (state) {
            case "LOW_BALANCE": return new LowBalanceMeterState();
            case "INACTIVE":    return new InactiveMeterState();
            default:            return new ActiveMeterState();
        }
    }

    public static MeterState resolve(double balance, double threshold) {
        if (balance <= 0)           return new InactiveMeterState();
        if (balance < threshold)    return new LowBalanceMeterState();
        return new ActiveMeterState();
    }
}