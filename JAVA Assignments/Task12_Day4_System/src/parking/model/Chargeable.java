package parking.model;

/** Capability of an electric vehicle to draw charge at a charging slot. */
public interface Chargeable {
    double CHARGE_RATE_PER_KWH = 12.0;

    double chargingFee(int kWh);
}
