package parking.app;

import parking.model.*;
import parking.service.*;

import java.time.LocalTime;

public class CampusApp {
    public static void main(String[] args) {
        // Day 4: five floors with different numbers of slots (jagged layout)
        CampusParkingService svc = new CampusParkingService(new int[]{50, 50, 60, 80, 100});
        HourlyRule hourly = new HourlyRule();
        ParkingTicket t1 = svc.enter("TWO_WHEELER", "tn01ab1111", LocalTime.of(9, 0), hourly);
        ParkingTicket t2 = svc.enter("CAR", "TN02CD2222", LocalTime.of(9, 15), hourly);
        ParkingTicket t3 = svc.enter("EV", "TN03EF3333", LocalTime.of(10, 0), hourly);
        ParkingTicket t4 = svc.enter("VAN", "TN04VN4444", LocalTime.of(8, 30), hourly);
        ParkingTicket t5 = svc.enter("EBIKE", "TN05EB5555", LocalTime.of(12, 0), hourly);
        ParkingTicket t6 = svc.enter("CAR", "TN06VS6666", LocalTime.of(10, 0), new DayPassRule());
        System.out.println(t1);
        System.out.println(t4);
        System.out.println("fee " + svc.exit(t1.getTicketNo(), LocalTime.of(10, 30), 0));
        System.out.println("fee " + svc.exit(t2.getTicketNo(), LocalTime.of(12, 0), 0));
        System.out.println("fee " + svc.exit(t3.getTicketNo(), LocalTime.of(11, 0), 8));
        System.out.println("fee " + svc.exit(t4.getTicketNo(), LocalTime.of(10, 0), 0));
        System.out.println("fee " + svc.exit(t5.getTicketNo(), LocalTime.of(20, 0), 0));
        System.out.println("fee " + svc.exit(t6.getTicketNo(), LocalTime.of(16, 0), 0));
        System.out.println("collection " + svc.getDailyCollection());
    }
}
