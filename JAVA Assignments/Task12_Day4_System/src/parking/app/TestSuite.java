package parking.app;

import parking.model.*;
import parking.service.*;

import java.time.LocalTime;

/** Test table (Day 4): expected and actual results with a PASS/FAIL verdict. */
public class TestSuite {
    private static int passed = 0, failed = 0;
    private static final HourlyRule HOURLY = new HourlyRule();

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
        // --- Day-1 behaviour, unchanged ---
        check("N1", "two-wheeler 90 min = 2 h x 10", 20.0, HOURLY.fee(new TwoWheeler("TN01"), null, 90));
        check("N2", "car 165 min = 3 h x 30", 90.0, HOURLY.fee(new Car("TN02"), null, 165));
        check("N3", "EV 60 min + 8 kWh = 30 + 96", 126.0,
              HOURLY.fee(new ElectricCar("TN03"), null, 60) + new ElectricCar("TN03").chargingFee(8));
        check("B1", "zero-minute stay is free", 0.0, HOURLY.fee(new Car("TN04"), null, 0));
        check("B2", "exactly 60 minutes charges 1 hour", 30.0, HOURLY.fee(new Car("TN04"), null, 60));
        check("B3", "61 minutes charges 2 hours (slab boundary)", 60.0, HOURLY.fee(new Car("TN04"), null, 61));
        check("N4", "12-hour car stay capped at Rs 300 (360 uncapped)", 300.0,
              HOURLY.fee(new Car("TN07"), LocalTime.of(6, 0), 720));
        check("N5", "monthly-pass holder pays nothing", 0.0,
              new MonthlyPassRule().fee(new Car("TN08"), null, 600));

        // --- Day-4 vehicle categories and pricing ---
        check("D1", "e-bike 10 hours = flat Rs 40", 40.0, HOURLY.fee(new EBike("TN11"), null, 600));
        check("D2", "e-bike 5 minutes = flat Rs 40", 40.0, HOURLY.fee(new EBike("TN11"), null, 5));
        check("D3", "van 08:30 for 90 min: 08:30 off-peak 30 + 09:30 peak 60", 90.0,
              HOURLY.fee(new DeliveryVan("TN12"), LocalTime.of(8, 30), 90));
        check("D4", "van 16:30 for 120 min: 16:30 peak 60 + 17:30 off-peak 30", 90.0,
              HOURLY.fee(new DeliveryVan("TN12"), LocalTime.of(16, 30), 120));
        check("D5", "van entering at 09:00 exactly is peak (60 per hour)", 60.0,
              HOURLY.fee(new DeliveryVan("TN12"), LocalTime.of(9, 0), 60));
        check("D6", "van entering at 17:00 is off-peak (30)", 30.0,
              HOURLY.fee(new DeliveryVan("TN12"), LocalTime.of(17, 0), 60));
        check("D9", "overload calculateFee(in, out): car 09:00-10:30 = 2 h x 30", 60.0,
              new Car("TN14").calculateFee(LocalTime.of(9, 0), LocalTime.of(10, 30)));
        check("D7", "one-day pass is a flat Rs 150 for a 3-hour stay", 150.0,
              new DayPassRule().fee(new Car("TN13"), LocalTime.of(10, 0), 180));
        check("D8", "one-day pass is a flat Rs 150 for a 10-hour stay", 150.0,
              new DayPassRule().fee(new Car("TN13"), LocalTime.of(8, 0), 600));

        // --- service rules with pricing rules ---
        CampusParkingService c = new CampusParkingService(new int[]{5});
        ParkingTicket long12 = c.enter("CAR", "TN07", LocalTime.of(6, 0), HOURLY);
        check("N6", "service: 12-hour car stay capped at Rs 300", 300.0,
              c.exit(long12.getTicketNo(), LocalTime.of(18, 0), 0));
        ParkingTicket pass = c.enter("EV", "TN08", LocalTime.of(8, 0), new MonthlyPassRule());
        check("N7", "service: monthly pass pays no parking and no charging", 0.0,
              c.exit(pass.getTicketNo(), LocalTime.of(17, 0), 8));

        // --- last free slot and full facility ---
        CampusParkingService s = new CampusParkingService(new int[]{1});
        ParkingTicket only = s.enter("CAR", "TN05", LocalTime.of(8, 0), HOURLY);
        check("B4", "last free slot is taken; facility now full", "[0, 0]",
              java.util.Arrays.toString(new int[]{only.getFloor(), only.getSlot()}));
        expectException("B5", "entry into a full facility is refused", IllegalStateException.class,
              () -> s.enter("CAR", "TN06", LocalTime.of(8, 5), HOURLY));

        // --- invalid cases ---
        expectException("I1", "unknown vehicle type is rejected", IllegalArgumentException.class,
              () -> CampusParkingService.createVehicle("TRUCK", "TN09"));
        CampusParkingService d = new CampusParkingService(new int[]{3});
        ParkingTicket tn10 = d.enter("CAR", "TN10", LocalTime.of(9, 0), HOURLY);
        expectException("I2", "duplicate registration number is rejected", IllegalArgumentException.class,
              () -> d.enter("TWO_WHEELER", "tn10", LocalTime.of(9, 5), HOURLY));
        check("I3", "invalid slot release (floor 5) returns false", false, d.getFacility().release(5, 0));
        check("I4", "releasing an already-free slot returns false", false, d.getFacility().release(0, 2));
        expectException("I5", "exit before entry is rejected", IllegalArgumentException.class,
              () -> d.exit(tn10.getTicketNo(), LocalTime.of(8, 0), 0));
        expectException("I6", "exit of an unknown ticket is rejected", IllegalArgumentException.class,
              () -> d.exit(999, LocalTime.of(10, 0), 0));

        System.out.println();
        System.out.println("passed=" + passed + " failed=" + failed);
    }
}
