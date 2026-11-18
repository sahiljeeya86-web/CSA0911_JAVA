package parking.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

/** Abstract vehicle: holds the registration number and the hourly parking rate. */
public abstract class Vehicle {
    private final String regNo;

    protected Vehicle(String regNo) {
        if (regNo == null || regNo.isBlank()) {
            throw new IllegalArgumentException("registration number is required");
        }
        this.regNo = regNo.trim().toUpperCase();
    }

    public String getRegNo() { return regNo; }

    /** Hourly rate in rupees; each subclass decides its own rate (overriding). */
    protected abstract double ratePerHour();

    /** Parking fee for a stay of the given minutes; every started hour is charged. */
    public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("minutes cannot be negative");
        int hours = (minutes + 59) / 60;          // integer ceiling of minutes / 60
        return hours * ratePerHour();
    }

    /**
     * Fee that may depend on when the stay starts (Day 4: time-of-day pricing).
     * The default ignores the entry time, so existing vehicle classes need no change.
     */
    public double fee(LocalTime entry, int minutes) {
        return calculateFee(minutes);
    }

    /**
     * Overload (Task 8): same name as calculateFee(int) but different parameter types.
     * The compiler chooses between the two at compile time from the argument types.
     */
    public double calculateFee(LocalTime in, LocalTime out) {
        if (out.isBefore(in)) throw new IllegalArgumentException("exit before entry");
        return fee(in, (int) Duration.between(in, out).toMinutes());
    }

    public String getType() { return getClass().getSimpleName(); }

    @Override public boolean equals(Object o) {
        return o instanceof Vehicle && regNo.equals(((Vehicle) o).regNo);
    }

    @Override public int hashCode() { return Objects.hash(regNo); }

    @Override public String toString() { return getType() + "[" + regNo + "]"; }
}
