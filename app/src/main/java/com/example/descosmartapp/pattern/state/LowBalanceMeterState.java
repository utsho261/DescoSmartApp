package com.example.descosmartapp.pattern.state;

import android.graphics.Color;

public class LowBalanceMeterState implements MeterState {
    @Override public String getStateName() { return "LOW_BALANCE"; }
    @Override public String getStateLabel() { return "ব্যালেন্স কম!"; }
    @Override public int getStateColor() { return Color.parseColor("#E65100"); }
    @Override public boolean canRecharge() { return true; }
    @Override public boolean showAlert() { return true; }
}