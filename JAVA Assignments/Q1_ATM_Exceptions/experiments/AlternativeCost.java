import java.util.*;

/** Q1 alternative: exceptions versus error codes; cost of creating and throwing exceptions. */
public class AlternativeCost {
    static long sink = 0;

    static long parseWithException(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return -1; }
    }
    static long parseWithCheck(String s) {
        int i = 0;
        if (s.isEmpty()) return -1;
        if (s.charAt(0) == '-') { if (s.length() == 1) return -1; i = 1; }
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return -1;
        }
        return Long.parseLong(s);
    }

    static double medianNs(Runnable r, int reps) {
        r.run(); r.run();
        double[] t = new double[5];
        for (int k = 0; k < 5; k++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) r.run();
            t[k] = (System.nanoTime() - t0) / (double) reps;
        }
        Arrays.sort(t);
        return t[2];
    }

    static void deep(int depth, Runnable leaf) {
        if (depth == 0) leaf.run(); else deep(depth - 1, leaf);
    }

    public static void main(String[] args) {
        String[] valid = {"1001", "250", "-7", "999999"};
        String[] invalid = {"abc", "12x", "", "-", "1.5"};
        int reps = 200_000;

        System.out.println("== A. Parsing: exceptions versus error codes (ns per call) ==");
        for (String kind : new String[]{"valid input", "invalid input"}) {
            String[] in = kind.equals("valid input") ? valid : invalid;
            double exc = medianNs(() -> { for (int i = 0; i < reps; i++) sink += parseWithException(in[i % in.length]); }, 1) / reps;
            double chk = medianNs(() -> { for (int i = 0; i < reps; i++) sink += parseWithCheck(in[i % in.length]); }, 1) / reps;
            System.out.printf("  %-14s try/catch: %8.1f ns   hand check + error code: %8.1f ns%n", kind, exc, chk);
        }

        System.out.println("== B. Cost of creating and throwing one exception (ns) ==");
        for (int depth : new int[]{1, 10, 100}) {
            double withTrace = medianNs(() -> deep(depth, () -> {
                try { throw new IllegalStateException("x"); } catch (IllegalStateException e) { sink += e.hashCode(); }
            }), 20_000);
            double noTrace = medianNs(() -> deep(depth, () -> {
                try { throw new NoTraceException(); } catch (NoTraceException e) { sink += e.hashCode(); }
            }), 20_000);
            System.out.printf("  stack depth %4d: with stack trace %10.0f ns, stack trace disabled %8.0f ns%n", depth, withTrace, noTrace);
        }
        double errorCode = medianNs(() -> { for (int i = 0; i < 20_000; i++) sink += -1; }, 1) / 20_000;
        System.out.printf("  returning an error code (for comparison): %.2f ns%n", errorCode);
        System.out.println("sink=" + sink);
    }

    static class NoTraceException extends RuntimeException {
        NoTraceException() { super("x", null, false, false); }
    }
}
