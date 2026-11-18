package parking.model;

import java.time.LocalTime;

/** Standard pay-per-hour rule with a daily maximum per stay. */
public class HourlyRule implements PricingRule {
    public static final double DAILY_CAP = 300;

    @Override public double fee(Vehicle vehicle, LocalTime entry, int minutes) {
        return Math.min(vehicle.fee(entry, minutes), DAILY_CAP);
    }

    @Override public String name() { return "HOURLY"; }
}
