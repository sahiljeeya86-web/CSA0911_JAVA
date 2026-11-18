package parking.model;

import java.time.LocalTime;

/** A ticket issued at entry. Ticket numbers come from a class-wide counter and are never reused. */
public class ParkingTicket {
    private static long nextTicketNo = 1;      // static: one counter for the whole class

    private final long ticketNo;
    private final Vehicle vehicle;
    private final int floor;
    private final int slot;
    private final LocalTime entryTime;
    private final boolean monthlyPass;         // staff and students with a valid monthly pass
    private LocalTime exitTime;                // null while the vehicle is parked

    public ParkingTicket(Vehicle vehicle, int floor, int slot, LocalTime entryTime, boolean monthlyPass) {
        if (vehicle == null || entryTime == null) throw new IllegalArgumentException("vehicle and entry time are required");
        if (floor < 0 || slot < 0) throw new IllegalArgumentException("invalid slot position");
        this.ticketNo = nextTicketNo++;
        this.vehicle = vehicle;
        this.floor = floor;
        this.slot = slot;
        this.entryTime = entryTime;
        this.monthlyPass = monthlyPass;
    }

    public long getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
    public LocalTime getEntryTime() { return entryTime; }
    public LocalTime getExitTime() { return exitTime; }
    public boolean hasMonthlyPass() { return monthlyPass; }

    public void recordExit(LocalTime exit) {
        if (exit == null || exit.isBefore(entryTime)) {
            throw new IllegalArgumentException("exit time cannot be before entry time");
        }
        this.exitTime = exit;
    }

    public long parkedMinutes() {
        if (exitTime == null) throw new IllegalStateException("vehicle has not left yet");
        return java.time.Duration.between(entryTime, exitTime).toMinutes();
    }

    @Override public String toString() {
        return "Ticket#" + ticketNo + " " + vehicle + " at " + floor + "/" + slot;
    }
}
