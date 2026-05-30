package com.example.descosmartapp.pattern.state;

import android.graphics.Color;

public class ActiveMeterState implements MeterState {
    @Override public String getStateName() { return "ACTIVE"; }
    @Override public String getStateLabel() { return "সক্রিয়"; }
    @Override public int getStateColor() { return Color.parseColor("#2E7D32"); }
    @Override public boolean canRecharge() { return true; }
    @Override public boolean showAlert() { return false; }
}