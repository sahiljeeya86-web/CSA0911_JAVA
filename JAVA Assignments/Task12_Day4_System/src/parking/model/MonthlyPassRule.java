package parking.model;

import java.time.LocalTime;

/** Staff and students holding a valid monthly pass park free of charge. */
public class MonthlyPassRule implements PricingRule {
    @Override public double fee(Vehicle vehicle, LocalTime entry, int minutes) { return 0; }

    @Override public boolean coversCharging() { return true; }

    @Override public String name() { return "MONTHLY_PASS"; }
}
