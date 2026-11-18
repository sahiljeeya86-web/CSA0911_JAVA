import java.util.*;

/** Q5 experiments: double bookings over many trials, and timing of 10,000 bookings per version. */
public class Race {
    interface Booking { void book(int seat); int sold(); int seatsMarked(); }

    static final class Plain implements Booking {
        final boolean[] booked; int bookingsMade = 0; final boolean yieldInWindow;
        Plain(int seats, boolean yieldInWindow) { booked = new boolean[seats]; this.yieldInWindow = yieldInWindow; }
        public void book(int seat) {
            if (!booked[seat]) {
                if (yieldInWindow) Thread.yield();       // widens the check-then-act window for the reproduction
                booked[seat] = true;
                bookingsMade++;
            }
        }
        public int sold() { return bookingsMade; }
        public int seatsMarked() { int n = 0; for (boolean b : booked) if (b) n++; return n; }
    }

    static final class SyncMethod implements Booking {
        final boolean[] booked; int bookingsMade = 0;
        SyncMethod(int seats) { booked = new boolean[seats]; }
        public synchronized void book(int seat) {
            if (!booked[seat]) { booked[seat] = true; bookingsMade++; }
        }
        public synchronized int sold() { return bookingsMade; }
        public synchronized int seatsMarked() { int n = 0; for (boolean b : booked) if (b) n++; return n; }
    }

    /** Same work as SyncBlock, but the whole method (including the ticket string) is locked. */
    static final class SyncMethodWithWork implements Booking {
        final boolean[] booked; int bookingsMade = 0;
        SyncMethodWithWork(int seats) { booked = new boolean[seats]; }
        public synchronized void book(int seat) {
            String ticket = "seat-" + seat + "-price-" + (seat * 7 % 13);   // non-critical work, now inside the lock
            if (!booked[seat]) { booked[seat] = true; bookingsMade++; }
        }
        public synchronized int sold() { return bookingsMade; }
        public synchronized int seatsMarked() { int n = 0; for (boolean b : booked) if (b) n++; return n; }
    }

    static final class SyncBlock implements Booking {
        final boolean[] booked; int bookingsMade = 0;
        SyncBlock(int seats) { booked = new boolean[seats]; }
        public void book(int seat) {
            String ticket = "seat-" + seat + "-price-" + (seat * 7 % 13);   // non-critical work, outside the lock
            synchronized (this) {                                             // only the check-then-act is locked
                if (!booked[seat]) { booked[seat] = true; bookingsMade++; }
            }
        }
        public int sold() { synchronized (this) { return bookingsMade; } }
        public int seatsMarked() { synchronized (this) { int n = 0; for (boolean b : booked) if (b) n++; return n; } }
    }

    /** Runs one trial: 100 threads, each booking seat (i % 10). Returns double bookings (sales minus seats sold). */
    static int trial(java.util.function.Supplier<Booking> make) throws InterruptedException {
        Booking b = make.get();
        Thread[] ts = new Thread[100];
        for (int i = 0; i < ts.length; i++) { final int seat = i % 10; ts[i] = new Thread(() -> b.book(seat)); }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return b.sold() - b.seatsMarked();
    }

    static void reproduce(String label, java.util.function.Supplier<Booking> make, int trials) throws InterruptedException {
        int trialsWithDouble = 0, totalDouble = 0, worst = 0;
        for (int t = 0; t < trials; t++) {
            int d = trial(make);
            if (d > 0) trialsWithDouble++;
            totalDouble += d;
            worst = Math.max(worst, d);
        }
        System.out.printf("  %-34s trials with double booking: %5d / %d   total double bookings: %6d   worst trial: %d%n",
                label, trialsWithDouble, trials, totalDouble, worst);
    }

    static long timeBookingsMicros(java.util.function.Supplier<Booking> make, int threads, int perThread) throws InterruptedException {
        Booking b = make.get();
        long t0 = System.nanoTime();
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                Random r = new Random(id);
                for (int k = 0; k < perThread; k++) b.book(r.nextInt(100_000));
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1_000;
    }

    static long timeBookings(java.util.function.Supplier<Booking> make, int threads, int perThread) throws InterruptedException {
        Booking b = make.get();
        long t0 = System.nanoTime();
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                Random r = new Random(id);
                for (int k = 0; k < perThread; k++) b.book(r.nextInt(100_000));
            });
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    public static void main(String[] args) throws InterruptedException {
        int trials = 2000;
        System.out.println("== A. Double booking over " + trials + " trials (100 users, 10 seats, each trial a new screen) ==");
        reproduce("A1 unsynchronized, as written", () -> new Plain(10, false), trials);
        reproduce("A2 unsynchronized, Thread.yield() in window", () -> new Plain(10, true), trials);
        reproduce("A3 synchronized method", () -> new SyncMethod(10), trials);
        reproduce("A4 synchronized block", () -> new SyncBlock(10), trials);

        System.out.println("== B. Time for 10,000 bookings (10 threads x 1,000 bookings on 100,000 seats), 200 screens per run, median of 5 runs ==");
        for (String kind : new String[]{"SyncMethod", "SyncMethodWithWork", "SyncBlock", "Plain (unsafe)"}) {
            long[] t = new long[5];
            for (int r = 0; r < 5; r++) {
                long total = 0;
                for (int screen = 0; screen < 200; screen++) {
                    java.util.function.Supplier<Booking> s = kind.equals("SyncMethod") ? () -> new SyncMethod(100_000)
                            : kind.equals("SyncMethodWithWork") ? () -> new SyncMethodWithWork(100_000)
                            : kind.equals("SyncBlock") ? () -> new SyncBlock(100_000) : () -> new Plain(100_000, false);
                    total += timeBookingsMicros(s, 10, 1000);
                }
                t[r] = total / 200;
            }
            Arrays.sort(t);
            System.out.printf("  %-16s %8d us per 10,000 bookings (median of 5 runs)%n", kind, t[2]);
        }
    }
}
