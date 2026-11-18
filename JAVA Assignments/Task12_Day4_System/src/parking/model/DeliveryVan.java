package parking.model;

import java.time.LocalTime;

/**
 * Delivery van (Day 4): car rate normally, double the car rate for each billed hour that
 * starts between 09:00 and 17:00. Each billed hour is priced by the time it starts.
 */
public class DeliveryVan extends Vehicle {
    public static final int PEAK_START_HOUR = 9;
    public static final int PEAK_END_HOUR = 17;

    public DeliveryVan(String regNo) { super(regNo); }

    @Override protected double ratePerHour() { return 30; }     // car rate

    /** Used when the entry time is unknown: off-peak rate for every hour. */
    @Override public double calculateFee(int minutes) {
        return super.calculateFee(minutes);
    }

    @Override public double fee(LocalTime entry, int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("minutes cannot be negative");
        int hours = (minutes + 59) / 60;
        double total = 0;
        for (int k = 0; k < hours; k++) {
            LocalTime start = entry.plusMinutes(60L * k);
            boolean peak = start.getHour() >= PEAK_START_HOUR && start.getHour() < PEAK_END_HOUR;
            total += peak ? 2 * ratePerHour() : ratePerHour();
        }
        return total;
    }
}
