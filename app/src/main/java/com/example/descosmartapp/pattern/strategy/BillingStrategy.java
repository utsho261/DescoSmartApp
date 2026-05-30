package com.example.descosmartapp.pattern.strategy;

/**
 * STRATEGY PATTERN — Different billing calculation strategies
 */
public interface BillingStrategy {
    double calculate(double units);
    String getStrategyName();
}