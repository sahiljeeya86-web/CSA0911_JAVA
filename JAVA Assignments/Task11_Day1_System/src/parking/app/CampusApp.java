package parking.app;

import parking.model.*;
import parking.service.*;

import java.time.LocalTime;

public class CampusApp {
    public static void main(String[] args) {
        CampusParkingService svc = new CampusParkingService(new int[]{50, 50, 50});
        ParkingTicket t1 = svc.enter("TWO_WHEELER", "tn01ab1111", LocalTime.of(9, 0), false);
        ParkingTicket t2 = svc.enter("CAR", "TN02CD2222", LocalTime.of(9, 15), false);
        ParkingTicket t3 = svc.enter("EV", "TN03EF3333", LocalTime.of(10, 0), false);
        System.out.println(t1);
        System.out.println(t2);
        System.out.println(t3);
        System.out.println("fee " + svc.exit(t1.getTicketNo(), LocalTime.of(10, 30), 0));
        System.out.println("fee " + svc.exit(t2.getTicketNo(), LocalTime.of(12, 0), 0));
        System.out.println("fee " + svc.exit(t3.getTicketNo(), LocalTime.of(11, 0), 8));
        System.out.println("collection " + svc.getDailyCollection());
    }
}
