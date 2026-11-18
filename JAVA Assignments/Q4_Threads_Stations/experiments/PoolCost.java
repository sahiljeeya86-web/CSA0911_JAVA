import java.util.*;
import java.util.concurrent.*;

/** Q4 alternative: n scripts, each taking about 10 ms of waiting (I/O) or of CPU work, on p threads. */
public class PoolCost {
    static void ioScript() { try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    static long sink = 0;
    static void cpuScript() { long x = 0; for (int i = 0; i < 1_500_000; i++) x += i ^ (x >>> 3); sink += x; }

    static long pool(int n, int p, Runnable script) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(p);
        long t0 = System.nanoTime();
        List<Future<?>> fs = new ArrayList<>();
        for (int i = 0; i < n; i++) fs.add(ex.submit(script));
        for (Future<?> f : fs) f.get();
        ex.shutdown();
        ex.awaitTermination(1, TimeUnit.MINUTES);
        return (System.nanoTime() - t0) / 1_000_000;
    }

    static long perThread(int n, Runnable script) throws Exception {
        long t0 = System.nanoTime();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < n; i++) { Thread t = new Thread(script); ts.add(t); t.start(); }
        for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    public static void main(String[] args) throws Exception {
        int n = 200;
        System.out.println("== I/O-like scripts (10 ms waiting each), n = " + n + " ==");
        System.out.println("  threads  time (ms)   expected ~ n*10/p");
        for (int p : new int[]{1, 2, 4, 8, 16}) {
            System.out.printf("  %7d  %9d   %d%n", p, pool(n, p, PoolCost::ioScript), n * 10 / p);
        }
        System.out.println("  one thread per script: " + perThread(n, PoolCost::ioScript) + " ms (" + n + " threads created)");
        System.out.println();
        System.out.println("== CPU-bound scripts, n = 40 (the machine has " + Runtime.getRuntime().availableProcessors() + " core) ==");
        for (int p : new int[]{1, 2, 4, 8}) {
            System.out.printf("  pool %d threads: %d ms%n", p, pool(40, p, PoolCost::cpuScript));
        }
        System.out.println("  one thread per script: " + perThread(40, PoolCost::cpuScript) + " ms");
        System.out.println("sink=" + sink);
    }
}
