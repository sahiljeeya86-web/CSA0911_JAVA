package parking.app;

import parking.model.*;
import parking.service.*;

import java.time.LocalTime;
import java.util.*;

/** Measurements for the "Alternative Concept and Complexity Analysis" parts. */
public class Analysis {

    static long sink = 0;   // prevents the JIT from removing the work being timed

    static double medianNs(Runnable r, int reps) {
        for (int i = 0; i < 3; i++) r.run();                  // warm-up
        double[] t = new double[7];
        for (int k = 0; k < t.length; k++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) r.run();
            t[k] = (System.nanoTime() - t0) / (double) reps;
        }
        Arrays.sort(t);
        return t[t.length / 2];
    }

    public static void main(String[] args) {
        System.out.println("== Task 2: integer division and casting ==");
        int minutes = 130;
        System.out.println("130 / 60 * 30            = " + (minutes / 60 * 30) + "   (wrong: integer division drops the partial hour)");
        System.out.println("(130 + 59) / 60 * 30     = " + ((minutes + 59) / 60 * 30) + "   (correct ceiling in integers)");
        System.out.println("(int)(130 / 60.0 * 30)   = " + (int) (minutes / 60.0 * 30) + "   (a proportional fee, not an hourly one: a different billing rule)");
        System.out.println("(int)(130 / 60.0) * 30   = " + (int) (minutes / 60.0) * 30 + "   (cast applied too early)");
        System.out.println("ceil(130 / 60.0) * 30    = " + Math.ceil(minutes / 60.0) * 30 + "   (floating point, avoided in the design)");
        System.out.println("(byte) 200 = " + (byte) 200 + "   narrowing cast wraps silently; slot numbers use int");

        System.out.println();
        System.out.println("== Task 5: static counter versus instance counter ==");
        CampusParkingService north = new CampusParkingService(new int[]{2});
        CampusParkingService south = new CampusParkingService(new int[]{2});
        ParkingTicket a = north.enter("CAR", "TN01", LocalTime.of(9, 0), new HourlyRule());
        ParkingTicket b = south.enter("CAR", "TN02", LocalTime.of(9, 0), new HourlyRule());
        System.out.println("static counter: north ticket " + a.getTicketNo() + ", south ticket " + b.getTicketNo() + " (unique across facilities)");
        int instanceNorth = 1, instanceSouth = 1;   // what an instance counter would give
        System.out.println("instance counters: north ticket " + instanceNorth + ", south ticket " + instanceSouth + " (duplicate numbers)");

        System.out.println();
        System.out.println("== Task 1 / Task 11: search cost, n parked vehicles (ns per search, missing key = worst case) ==");
        System.out.printf("%-9s %-22s %-22s %-22s%n", "n", "objects (ArrayList)", "parallel String[]", "HashMap<String,..>");
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            List<ParkingTicket> list = new ArrayList<>();
            String[] regs = new String[n];
            Map<String, ParkingTicket> map = new HashMap<>();
            for (int i = 0; i < n; i++) {
                String r = String.format("TN%06d", i);
                regs[i] = r;
                ParkingTicket t = new ParkingTicket(new TwoWheeler(r), 0, 0, LocalTime.of(9, 0), new HourlyRule());
                list.add(t);
                map.put(r, t);
            }
            String missing = "ZZ999999";
            int reps = Math.max(20, 2_000_000 / n);
            double tList = medianNs(() -> {
                for (ParkingTicket t : list) if (t.getVehicle().getRegNo().equals(missing)) sink++;
            }, reps);
            double tArr = medianNs(() -> {
                for (int i = 0; i < regs.length; i++) if (regs[i].equals(missing)) sink++;
            }, reps);
            double tMap = medianNs(() -> { if (map.get(missing) != null) sink++; }, reps * 10);
            System.out.printf("%-9d %-22.1f %-22.1f %-22.1f%n", n, tList, tArr, tMap);
        }

        System.out.println();
        System.out.println("== Task 3: allocation with a scan versus a stack of free slots (ns per allocate, filling the facility) ==");
        for (int[] shape : new int[][]{{5, 100}, {50, 1000}}) {
            int floors = shape[0], perFloor = shape[1];
            int[] layout = new int[floors];
            Arrays.fill(layout, perFloor);
            double scanNs = medianNs(() -> {
                ParkingFacility f = new ParkingFacility(layout);
                for (int i = 0; i < floors * perFloor; i++) f.allocate("X" + i);
            }, 3) / (floors * perFloor);
            double stackNs = medianNs(() -> {
                Deque<Integer> free = new ArrayDeque<>();
                for (int i = floors * perFloor - 1; i >= 0; i--) free.push(i);
                int[] occupant = new int[floors * perFloor];
                for (int i = 0; i < floors * perFloor; i++) { int s = free.pop(); occupant[s] = i + 1; }
                sink += occupant[0];
            }, 3) / (floors * perFloor);
            System.out.printf("F=%d S=%d  scan %.1f ns/allocate, stack %.1f ns/allocate (includes stack setup)%n",
                    floors, perFloor, scanNs, stackNs);
        }

        System.out.println();
        System.out.println("== Task 4: creating n tickets, constructor versus default constructor plus setters ==");
        int n = 1_000_000;
        double tCtor = medianNs(() -> {
            for (int i = 0; i < n / 100; i++) sink += new ParkingTicket(new TwoWheeler("T" + i), 0, 0, LocalTime.of(9, 0), new HourlyRule()).getFloor();
        }, 1) / (n / 100);
        System.out.printf("constructor with all fields: %.1f ns per object (valid object at creation, no extra calls)%n", tCtor);
        System.out.println("default constructor + setters: same O(1) per object, but 4 extra calls and a window where the object is incomplete");

        System.out.println();
        System.out.println("== Task 6 / Task 7: fee by overriding (virtual call) versus a switch on a type code (ns per call) ==");
        Vehicle[] vehicles = {new TwoWheeler("A"), new Car("B"), new DeliveryVan("C"), new EBike("D")};
        int[] codes = {0, 1, 2, 3};
        double tVirt = medianNs(() -> {
            for (Vehicle v : vehicles) sink += (long) v.calculateFee(130);
        }, 200_000) / vehicles.length;
        double tSwitch = medianNs(() -> {
            for (int c : codes) {
                switch (c) {
                    case 0: sink += (long) (3 * 10); break;
                    case 1: sink += (long) (3 * 30); break;
                    case 2: sink += (long) (3 * 30); break;
                    default: sink += 40;
                }
            }
        }, 200_000) / codes.length;
        System.out.printf("virtual calculateFee(): %.2f ns per call; switch on type code: %.2f ns per call%n", tVirt, tSwitch);

        System.out.println();
        System.out.println("== Task 8: dispatch versus an instanceof chain (ns per object; both compute the same fee for 130 minutes) ==");
        Object[] objs = {new TwoWheeler("A"), new Car("B"), new DeliveryVan("C"), new EBike("D"), new ElectricCar("E")};
        double tChain = medianNs(() -> {
            for (Object o : objs) {
                int hours = (130 + 59) / 60;
                if (o instanceof TwoWheeler t) sink += (long) (hours * 10);
                else if (o instanceof EBike e) sink += (long) 40;
                else if (o instanceof DeliveryVan v) sink += (long) (hours * 30);
                else if (o instanceof ElectricCar ec) sink += (long) (hours * 30);
                else if (o instanceof Car c) sink += (long) (hours * 30);
            }
        }, 200_000) / objs.length;
        Object[] objs2 = {new TwoWheeler("A"), new EBike("D"), new DeliveryVan("C"), new ElectricCar("E"), new Car("B")};
        double tDispatch = medianNs(() -> {
            for (Object o : objs2) sink += (long) ((Vehicle) o).calculateFee(130);
        }, 200_000) / objs2.length;
        System.out.printf("instanceof chain (5 tests): %.2f ns per object; virtual dispatch: %.2f ns per call%n", tChain, tDispatch);

        System.out.println();
        System.out.println("sink=" + sink);
    }
}
