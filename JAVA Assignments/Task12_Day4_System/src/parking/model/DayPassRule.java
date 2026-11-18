package parking.model;

import java.time.LocalTime;

/** Visitor one-day pass (Day 4): a flat price for the whole day, whatever the duration. */
public class DayPassRule implements PricingRule {
    public static final double PRICE = 150;

    @Override public double fee(Vehicle vehicle, LocalTime entry, int minutes) { return PRICE; }

    @Override public String name() { return "DAY_PASS"; }
}
