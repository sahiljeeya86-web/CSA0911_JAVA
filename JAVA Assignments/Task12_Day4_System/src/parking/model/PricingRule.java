package parking.model;

import java.time.LocalTime;

/** How a parking stay is charged. Adding a new rule needs no change to existing classes. */
public interface PricingRule {
    /** Parking fee in rupees for a stay of the given minutes that began at entry. */
    double fee(Vehicle vehicle, LocalTime entry, int minutes);

    /** True when the stay is free of the parking charge, so EV charging is also free. */
    default boolean coversCharging() { return false; }

    String name();
}
