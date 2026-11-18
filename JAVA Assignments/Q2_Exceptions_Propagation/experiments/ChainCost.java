import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/** Q2 alternative: cost and retained memory of exception chains, with and without stack traces. */
public class ChainCost {
    static final ThreadMXBean MX = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    static Object keep;

    static class Checked extends Exception { Checked(String m, Throwable c) { super(m, c); } }
    static class Unchecked extends RuntimeException { Unchecked(String m, Throwable c) { super(m, c); } }
    static class NoTrace extends RuntimeException {
        NoTrace(String m, Throwable c) { super(m, c, false, false); }
    }

    static void deep(int d, Runnable r) { if (d == 0) r.run(); else deep(d - 1, r); }

    static long bytesPerCreate(int depth, boolean trace, int chain) {
        long a0 = MX.getThreadAllocatedBytes(Thread.currentThread().getId());
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            final int k = i;
            deep(depth, () -> {
                Throwable cause = null;
                for (int c = 0; c < chain; c++) {
                    cause = trace ? new Checked("c" + c, cause) : new NoTrace("c" + c, cause);
                }
                keep = cause;
            });
        }
        return (MX.getThreadAllocatedBytes(Thread.currentThread().getId()) - a0) / n;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) { bytesPerCreate(10, true, 1); bytesPerCreate(10, false, 1); }
        System.out.println("bytes allocated per exception object chain (median-free, one run, 10,000 creations each)");
        System.out.println("  depth  chain  trace   bytes per chain");
        for (int depth : new int[]{1, 10, 100}) {
            for (int chain : new int[]{1, 3, 5}) {
                System.out.printf("  %5d  %5d  yes     %8d%n", depth, chain, bytesPerCreate(depth, true, chain));
                System.out.printf("  %5d  %5d  no      %8d%n", depth, chain, bytesPerCreate(depth, false, chain));
            }
        }
    }
}
