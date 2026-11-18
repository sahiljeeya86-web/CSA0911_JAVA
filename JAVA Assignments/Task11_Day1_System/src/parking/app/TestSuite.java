package parking.app;

import parking.model.*;
import parking.service.*;

import java.time.LocalTime;

/** Test table: each row prints the expected and actual result and a PASS/FAIL verdict. */
public class TestSuite {
    private static int passed = 0, failed = 0;

    static void check(String id, String description, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) passed++; else failed++;
        System.out.printf("%-5s %-52s expected=%-12s actual=%-12s %s%n",
                id, description, expected, actual, ok ? "PASS" : "FAIL");
    }

    static void expectException(String id, String description, Class<?> type, Runnable action) {
        try {
            action.run();
            check(id, description, type.getSimpleName(), "no exception");
        } catch (RuntimeException ex) {
            check(id, description, type.getSimpleName(), ex.getClass().getSimpleName());
        }
    }

    public static void main(String[] args) {
        System.out.println("ID    DESCRIPTION                                          RESULT");
        // --- normal operation: fees by vehicle type ---
        check("N1", "two-wheeler 90 min = 2 h x 10", 20.0,
              new TwoWheeler("TN01").calculateFee(90));
        check("N2", "car 165 min = 3 h x 30", 90.0, new Car("TN02").calculateFee(165));
        check("N3", "EV 60 min + 8 kWh = 30 + 96", 126.0,
              new ElectricCar("TN03").calculateFee(60) + new ElectricCar("TN03").chargingFee(8));

        // --- boundary cases ---
        check("B1", "zero-minute stay is free", 0.0, new Car("TN04").calculateFee(0));
        check("B2", "exactly 60 minutes charges 1 hour", 30.0, new Car("TN04").calculateFee(60));
        check("B3", "61 minutes charges 2 hours (slab boundary)", 60.0, new Car("TN04").calculateFee(61));

        // --- service rules ---
        CampusParkingService s = new CampusParkingService(new int[]{1});
        ParkingTicket only = s.enter("CAR", "TN05", LocalTime.of(8, 0), false);
        check("B4", "last free slot is taken; facility now full", "[0, 0]",
              java.util.Arrays.toString(new int[]{only.getFloor(), only.getSlot()}));
        expectException("B5", "entry into a full facility is refused", IllegalStateException.class,
              () -> s.enter("CAR", "TN06", LocalTime.of(8, 5), false));

        CampusParkingService c = new CampusParkingService(new int[]{5});
        ParkingTicket long12 = c.enter("CAR", "TN07", LocalTime.of(6, 0), false);
        check("N4", "12-hour car stay is capped at Rs 300 (360 uncapped)", 300.0,
              c.exit(long12.getTicketNo(), LocalTime.of(18, 0), 0));

        ParkingTicket pass = c.enter("CAR", "TN08", LocalTime.of(8, 0), true);
        check("N5", "monthly-pass holder pays nothing", 0.0,
              c.exit(pass.getTicketNo(), LocalTime.of(17, 0), 0));

        // --- invalid cases ---
        expectException("I1", "unknown vehicle type is rejected", IllegalArgumentException.class,
              () -> CampusParkingService.createVehicle("TRUCK", "TN09"));
        CampusParkingService d = new CampusParkingService(new int[]{3});
        ParkingTicket tn10 = d.enter("CAR", "TN10", LocalTime.of(9, 0), false);
        expectException("I2", "duplicate registration number is rejected", IllegalArgumentException.class,
              () -> d.enter("TWO_WHEELER", "tn10", LocalTime.of(9, 5), false));
        check("I3", "invalid slot release (floor 5) returns false", false,
              d.getFacility().release(5, 0));
        check("I4", "releasing an already-free slot returns false", false,
              d.getFacility().release(0, 2));
        expectException("I5", "exit before entry is rejected", IllegalArgumentException.class,
              () -> d.exit(tn10.getTicketNo(), LocalTime.of(8, 0), 0));
        expectException("I6", "exit of an unknown ticket is rejected", IllegalArgumentException.class,
              () -> d.exit(999, LocalTime.of(10, 0), 0));

        System.out.println();
        System.out.println("passed=" + passed + " failed=" + failed);
    }
}
