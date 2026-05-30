package com.example.descosmartapp.pattern.strategy;

/** DESCO Residential Tariff (BDT/unit slab) */
public class ResidentialBillingStrategy implements BillingStrategy {

    @Override
    public double calculate(double units) {
        double bill = 0;
        if (units <= 50)       bill = units * 3.75;
        else if (units <= 75)  bill = 50*3.75 + (units-50)*4.19;
        else if (units <= 200) bill = 50*3.75 + 25*4.19 + (units-75)*5.72;
        else if (units <= 300) bill = 50*3.75 + 25*4.19 + 125*5.72 + (units-200)*6.00;
        else if (units <= 400) bill = 50*3.75 + 25*4.19 + 125*5.72 + 100*6.00 + (units-300)*6.34;
        else if (units <= 600) bill = 50*3.75 + 25*4.19 + 125*5.72 + 100*6.00 + 100*6.34 + (units-400)*9.94;
        else                   bill = 50*3.75 + 25*4.19 + 125*5.72 + 100*6.00 + 100*6.34 + 200*9.94 + (units-600)*11.46;
        return Math.round(bill * 100.0) / 100.0;
    }

    @Override
    public String getStrategyName() { return "Residential"; }
}