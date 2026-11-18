import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;

/** Q1: building e-tickets with +, StringBuilder, StringBuffer, StringJoiner and String.format. */
public class TicketBench {
    static final ThreadMXBean MX = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long sink = 0;

    static String field(int i) { return "F" + i + "=val" + (i * 37 % 1000) + ";"; }

    static String buildConcat(int fields) {
        String s = "";
        for (int i = 0; i < fields; i++) s = s + field(i);        // one new String per field
        return s;
    }
    static String buildBuilder(int fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields; i++) sb.append(field(i));
        return sb.toString();
    }
    static String buildBuffer(int fields) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < fields; i++) sb.append(field(i));
        return sb.toString();
    }
    static String buildJoiner(int fields) {
        StringJoiner j = new StringJoiner("", "", "");
        for (int i = 0; i < fields; i++) j.add(field(i));
        return j.toString();
    }
    static String buildFormat(int fields) {
        String s = "";
        for (int i = 0; i < fields; i++) s = String.format("%s%s", s, field(i));
        return s;
    }

    interface Builder { String build(int fields); }

    /** Average time per ticket (ns) over repeated runs, and bytes allocated per ticket. */
    static double[] measure(Builder b, int tickets, int fields) {
        for (int t = 0; t < Math.min(tickets, 50); t++) sink += b.build(fields).length();
        double[] times = new double[3];
        for (int r = 0; r < 3; r++) {
            long t0 = System.nanoTime();
            for (int t = 0; t < tickets; t++) sink += b.build(fields).length();
            times[r] = (System.nanoTime() - t0) / (double) tickets;
        }
        Arrays.sort(times);
        long id = Thread.currentThread().getId();
        long a0 = MX.getThreadAllocatedBytes(id);
        for (int t = 0; t < tickets; t++) sink += b.build(fields).length();
        long bytes = MX.getThreadAllocatedBytes(id) - a0;
        return new double[]{times[1], bytes / (double) tickets};
    }

    /** Intermediate String objects and characters copied by the + loop (exact count). */
    static long[] concatCost(int fields) {
        long objects = 0, copied = 0, len = 0;
        for (int i = 0; i < fields; i++) {
            len += field(i).length();
            objects++;
            copied += len;          // the new string is a full copy of the previous text plus the field
        }
        return new long[]{objects, copied};
    }

    public static void main(String[] args) throws Exception {
        int[][] workloads = {{1, 10}, {10_000, 50}, {100, 500}};     // {tickets, fields per ticket}
        String[] names = {"W1 short ticket (1 ticket, 10 fields) x 20,000 repeats",
                          "W2 10,000 tickets x 50 fields", "W3 own: 100 tickets x 500 fields"};
        System.out.println("== Time per ticket (median of 5 runs) and bytes allocated per ticket ==");
        for (int wi = 0; wi < workloads.length; wi++) {
            int tickets = workloads[wi][0], fields = workloads[wi][1];
            int reps = wi == 0 ? 20_000 : 1;
            System.out.println(names[wi]);
            Map<String, Builder> approaches = new LinkedHashMap<>();
            approaches.put("+ concatenation", TicketBench::buildConcat);
            approaches.put("StringBuilder", TicketBench::buildBuilder);
            approaches.put("StringBuffer", TicketBench::buildBuffer);
            approaches.put("StringJoiner", TicketBench::buildJoiner);
            if (wi == 0) approaches.put("String.format (short only)", TicketBench::buildFormat);
            for (Map.Entry<String, Builder> e : approaches.entrySet()) {
                if (wi != 0 && e.getKey().contains("format")) continue;
                double[] m = wi == 0 ? measureRepeated(e.getValue(), reps) : measure(e.getValue(), tickets, fields);
                System.out.printf("  %-26s %14.1f ns/ticket   %12.1f bytes/ticket%n", e.getKey(), m[0], m[1]);
            }
            long[] c = concatCost(fields);
            System.out.printf("  + loop, exact: intermediate String objects per ticket = %d, characters copied per ticket = %d%n", c[0], c[1]);
            long L = 0; for (int i = 0; i < fields; i++) L += field(i).length();
            System.out.printf("  StringBuilder: intermediate String objects per ticket = 1 (final toString), final length L = %d, characters copied = about L (toString) plus at most about 2L during growth%n", L);
            System.out.println();
        }
        System.out.println("sink=" + sink);
        System.out.flush();
    }

    /** Average time with the given number of repeats, for the short workload. */
    static double[] measureRepeated(Builder b, int reps) {
        for (int i = 0; i < reps / 10; i++) sink += b.build(10).length();
        double[] times = new double[3];
        for (int r = 0; r < 3; r++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) sink += b.build(10).length();
            times[r] = (System.nanoTime() - t0) / (double) reps;
        }
        Arrays.sort(times);
        long id = Thread.currentThread().getId();
        long a0 = MX.getThreadAllocatedBytes(id);
        for (int i = 0; i < reps; i++) sink += b.build(10).length();
        return new double[]{times[1], (MX.getThreadAllocatedBytes(id) - a0) / (double) reps};
    }

    static String sharedLog(Appendable target, int lines, int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            fs.add(pool.submit(() -> {
                go.await();
                for (int i = 0; i < lines; i++) {
                    synchronized (target) {}                   // no lock for the StringBuilder case
                    if (target instanceof StringBuilder sb) sb.append("LINE-0000000001;");
                    else ((StringBuffer) target).append("LINE-0000000001;");
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        int len = target instanceof StringBuilder sb ? sb.length() : ((StringBuffer) target).length();
        long expected = (long) threads * lines * "LINE-0000000001;".length();
        return "length = " + len + ", lost characters = " + (expected - len) + " (" + (expected == len ? "correct" : "CORRUPTED") + ")";
    }
}
