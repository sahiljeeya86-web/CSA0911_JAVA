import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Q3 alternative: peak open file handles and time for n store files, three versions, with and without a failure. */
public class ReportCost {
    static String[] make(int n, int badAt) throws IOException {
        Path dir = Paths.get("stores_" + n + "_" + badAt);
        Files.createDirectories(dir);
        String[] f = new String[n];
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int r = 1; r <= 5; r++) sb.append("item,").append(i % 9 + 1).append(',').append(10 * r).append('\n');
            if (i == badAt) sb.append("bad,row\n");
            Path p = dir.resolve(String.format("s%05d.txt", i));
            Files.writeString(p, sb.toString());
            f[i] = p.toString();
        }
        return f;
    }

    static String run(String version, String[] files) {
        Tracked.resetCounters();
        long t0 = System.nanoTime();
        String outcome;
        try {
            switch (version) {
                case "manual in try body": Reports.manualLeaky(files); break;
                case "manual in finally": Reports.manualFinally(files); break;
                default: Reports.tryWithResources(files);
            }
            outcome = "ok";
        } catch (Exception e) {
            outcome = e.getMessage() != null && e.getMessage().contains("Too many open files") ? "Too many open files" : "failed";
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        return String.format("%-22s %-24s peak open=%5d  still open at end=%5d  time=%5d ms", version, outcome, Tracked.peak, Tracked.OPEN.size(), ms);
    }

    public static void main(String[] args) throws IOException {
        int n = Integer.parseInt(args[0]);
        int badAt = Integer.parseInt(args[1]);
        String version = args[2];
        String[] files = make(n, badAt);
        System.out.println("n=" + n + " badAt=" + badAt + " " + run(version, files));
    }
}
