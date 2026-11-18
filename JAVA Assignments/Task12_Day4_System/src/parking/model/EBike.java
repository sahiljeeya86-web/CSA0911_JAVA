package parking.model;

/** E-bike (Day 4): charged a flat fee per visit, whatever the duration. */
public class EBike extends Vehicle {
    public static final double FLAT_FEE = 40;

    public EBike(String regNo) { super(regNo); }

    @Override protected double ratePerHour() { return 0; }   // not used: flat fee below

    @Override public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("minutes cannot be negative");
        return FLAT_FEE;
    }
}
