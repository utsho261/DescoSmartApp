package com.example.descosmartapp.pattern.state;

/**
 * STATE PATTERN — Meter can be in different states
 */
public interface MeterState {
    String getStateName();
    String getStateLabel();   // Bengali label for UI
    int getStateColor();      // Color resource
    boolean canRecharge();
    boolean showAlert();
}