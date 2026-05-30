package com.example.descosmartapp.pattern.state;

import android.graphics.Color;

public class InactiveMeterState implements MeterState {
    @Override public String getStateName() { return "INACTIVE"; }
    @Override public String getStateLabel() { return "নিষ্ক্রিয়"; }
    @Override public int getStateColor() { return Color.parseColor("#B71C1C"); }
    @Override public boolean canRecharge() { return false; }
    @Override public boolean showAlert() { return true; }
}