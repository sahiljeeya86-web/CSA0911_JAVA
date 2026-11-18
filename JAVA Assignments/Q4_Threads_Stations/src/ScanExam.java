import java.util.*;

public class ScanExam {
    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "main";
        if (mode.equals("main")) runMain();
        else if (mode.equals("run-vs-start")) runVsStart();
        else if (mode.equals("priority")) runPriority();
        else if (mode.equals("daemon")) runDaemon();
        else if (mode.equals("race")) runRace();
    }

    static void runMain() throws InterruptedException {
        StationThread a = new StationThread("Station-A", 20, 5);
        StationThread b = new StationThread("Station-B", 15, 5);
        StationRunnable c = new StationRunnable("Station-C", 25, 5);
        Thread tc = new Thread(c, "Station-C");
        System.out.println("life cycle before start: " + a.getState() + ", isAlive=" + a.isAlive());
        a.start(); b.start(); tc.start();
        System.out.println("life cycle just after start: " + a.getState() + ", isAlive=" + a.isAlive());
        a.join(); b.join(); tc.join();
        System.out.println("life cycle after join: " + a.getState() + ", isAlive=" + a.isAlive() + ", " + tc.getState() + " isAlive=" + tc.isAlive());
        int total = a.scanned + b.scanned + c.scanned;
        System.out.println("TOTAL scripts scanned: " + total + " (expected " + (20 + 15 + 25) + ")");
    }

    static void runVsStart() throws InterruptedException {
        StationRunnable r = new StationRunnable("Station-R", 1, 0);
        System.out.println("calling run() directly in " + Thread.currentThread().getName());
        r.run();
        System.out.println("calling start() instead:");
        Thread t = new Thread(new StationRunnable("Station-S", 1, 0));
        t.start(); t.join();
    }

    static void runPriority() throws InterruptedException {
        // CPU-bound work with no sleep, so the scheduler decides the finish order
        Thread low = new Thread(() -> spin(), "low");
        Thread high = new Thread(() -> spin(), "high");
        low.setPriority(Thread.MIN_PRIORITY);
        high.setPriority(Thread.MAX_PRIORITY);
        low.start(); high.start();
        low.join(); high.join();
        System.out.println("priority run finished");
    }
    static void spin() {
        long x = 0;
        for (long i = 0; i < 300_000_000L; i++) x += i ^ (x >>> 3);
        System.out.println(Thread.currentThread().getName() + " (priority " + Thread.currentThread().getPriority() + ") done " + (x != 0));
    }

    static void runDaemon() throws InterruptedException {
        Thread worker = new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            System.out.println("daemon finished its work");
        }, "Station-D");
        worker.setDaemon(true);
        worker.start();
        System.out.println("main ends now, daemon isAlive=" + worker.isAlive());
    }

    /** Four identical stations with equal, CPU-only work: the order depends only on the scheduler. */
    static void runRace() throws InterruptedException {
        List<Thread> ts = new ArrayList<>();
        for (int k = 1; k <= 4; k++) {
            final String name = "Station-" + k;
            ts.add(new Thread(() -> {
                long x = 0;
                for (int i = 0; i < 40_000_000; i++) x += i ^ (x >>> 3);
                System.out.println(name + " done " + (x != 0));
            }, name));
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
    }
}
