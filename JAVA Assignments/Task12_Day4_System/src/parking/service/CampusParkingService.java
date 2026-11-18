package parking.service;

import parking.model.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Business rules: entry, exit, fee calculation through a PricingRule, and the day's collection. */
public class CampusParkingService {
    private final ParkingFacility facility;
    private final List<ParkingTicket> active = new ArrayList<>();   // linear search on exit
    private double dailyCollection = 0;

    public CampusParkingService(int[] slotsPerFloor) {
        this.facility = new ParkingFacility(slotsPerFloor);
    }

    public ParkingTicket enter(String type, String regNo, LocalTime time, PricingRule rule) {
        Vehicle vehicle = createVehicle(type, regNo);
        for (ParkingTicket t : active) {
            if (t.getVehicle().equals(vehicle)) {
                throw new IllegalArgumentException("vehicle " + vehicle.getRegNo() + " is already parked");
            }
        }
        int[] pos = facility.allocate(vehicle.getRegNo());
        if (pos == null) throw new IllegalStateException("no free slot in the facility");
        ParkingTicket ticket = new ParkingTicket(vehicle, pos[0], pos[1], time, rule);
        active.add(ticket);
        return ticket;
    }

    /** Returns the total fee charged for this visit (parking plus any EV charging). */
    public double exit(long ticketNo, LocalTime time, int kWhCharged) {
        ParkingTicket ticket = findActive(ticketNo);
        if (ticket == null) throw new IllegalArgumentException("unknown or already closed ticket " + ticketNo);
        ticket.recordExit(time);
        facility.release(ticket.getFloor(), ticket.getSlot());
        active.remove(ticket);

        double fee = ticket.getRule().fee(ticket.getVehicle(), ticket.getEntryTime(),
                                          (int) ticket.parkedMinutes());
        if (!ticket.getRule().coversCharging() && kWhCharged > 0
                && ticket.getVehicle() instanceof Chargeable c) {
            fee += c.chargingFee(kWhCharged);
        }
        dailyCollection += fee;
        return fee;
    }

    public double getDailyCollection() { return dailyCollection; }
    public int getActiveCount() { return active.size(); }
    public ParkingFacility getFacility() { return facility; }

    private ParkingTicket findActive(long ticketNo) {
        for (ParkingTicket t : active) if (t.getTicketNo() == ticketNo) return t;
        return null;
    }

    public static Vehicle createVehicle(String type, String regNo) {
        if (type == null) throw new IllegalArgumentException("vehicle type is required");
        switch (type.trim().toUpperCase()) {
            case "TWO_WHEELER": return new TwoWheeler(regNo);
            case "CAR":         return new Car(regNo);
            case "EV":          return new ElectricCar(regNo);
            case "EBIKE":       return new EBike(regNo);          // Day 4
            case "VAN":         return new DeliveryVan(regNo);    // Day 4
            default: throw new IllegalArgumentException("unknown vehicle type: " + type);
        }
    }
}
