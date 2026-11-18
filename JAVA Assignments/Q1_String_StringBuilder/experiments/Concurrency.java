import java.util.*;
import java.util.concurrent.*;

/** Q1: unsafe versus safe appends to a shared log, repeated to measure how often data is lost. */
public class Concurrency {
    static final String LINE = "LINE-0000000001;";

    static String run(StringBuilder sb, StringBuffer sf, boolean useBuffer, int threads, int perThread) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier start = new CyclicBarrier(threads);
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            fs.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    if (useBuffer) sf.append(LINE); else sb.append(LINE);
                }
                return null;
            }));
        }
        int exceptions = 0;
        for (Future<?> f : fs) {
            try { f.get(); } catch (ExecutionException e) { exceptions++; }
        }
        pool.shutdown();
        long expected = (long) threads * perThread * LINE.length();
        long got = useBuffer ? sf.length() : sb.length();
        return "expected=" + expected + " got=" + got + " lost=" + (expected - got)
             + " worker_exceptions=" + exceptions;
    }

    public static void main(String[] args) throws Exception {
        int threads = 8, perThread = 100_000, trials = 10;
        int bad = 0, badBuf = 0;
        System.out.println("8 threads x 100,000 appends of a 17-char line, " + trials + " trials each");
        for (int t = 0; t < trials; t++) {
            String r = run(new StringBuilder(), null, false, threads, perThread);
            if (!r.contains("lost=0 ") || !r.endsWith("worker_exceptions=0")) bad++;
            System.out.println("StringBuilder trial " + (t + 1) + ": " + r);
        }
        for (int t = 0; t < trials; t++) {
            String r = run(null, new StringBuffer(), true, threads, perThread);
            if (!r.contains("lost=0 ") || !r.endsWith("worker_exceptions=0")) badBuf++;
            System.out.println("StringBuffer trial " + (t + 1) + ": " + r);
        }
        System.out.println("trials with lost data or exceptions: StringBuilder " + bad + "/" + trials
                + ", StringBuffer " + badBuf + "/" + trials);
    }
}
