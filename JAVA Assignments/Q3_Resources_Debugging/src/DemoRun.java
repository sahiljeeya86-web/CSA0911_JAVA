import java.io.*;
import java.nio.file.*;

public class DemoRun {
    static void makeStores(int badIndex) throws IOException {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder();
            for (int r = 1; r <= 5; r++) sb.append("item").append(r).append(',').append(i + 1).append(',').append(10 * r).append('\n');
            if (i == badIndex) sb.append("bad,row\n");
            Files.writeString(Paths.get(String.format("store%02d.txt", i)), sb.toString());
        }
    }
    static String[] files() {
        String[] f = new String[10];
        for (int i = 0; i < 10; i++) f[i] = String.format("store%02d.txt", i);
        return f;
    }
    static void run(String label, java.util.function.Function<String[], Long> rep) {
        Tracked.resetCounters();
        System.out.println(label);
        try {
            long total = rep.apply(files());
            System.out.println("  total = " + total);
        } catch (Exception e) {
            System.out.println("  exception: " + e.getClass().getSimpleName());
        }
        System.out.println("  opened=" + Tracked.opened + " closed=" + Tracked.closed + " still open=" + Tracked.OPEN.size());
        for (String name : Tracked.OPEN.keySet()) { StackTraceElement at = Tracked.OPEN.get(name).getStackTrace()[1]; System.out.println("    LEAK " + name + "  opened at " + at.getClassName() + "." + at.getMethodName() + "(" + at.getFileName() + ":" + at.getLineNumber() + ")"); }
    }
    public static void main(String[] args) throws IOException {
        makeStores(-1);
        run("A. good input, manual close in try body", fs -> { try { return Reports.manualLeaky(fs); } catch (IOException e) { throw new RuntimeException(e); } });
        run("B. good input, manual close in finally", fs -> { try { return Reports.manualFinally(fs); } catch (IOException e) { throw new RuntimeException(e); } });
        run("C. good input, try-with-resources", fs -> { try { return Reports.tryWithResources(fs); } catch (IOException e) { throw new RuntimeException(e); } });
        makeStores(4);
        run("D. store04 malformed, manual close in try body", fs -> { try { return Reports.manualLeaky(fs); } catch (IOException e) { throw new RuntimeException(e); } });
        run("E. store04 malformed, manual close in finally", fs -> { try { return Reports.manualFinally(fs); } catch (IOException e) { throw new RuntimeException(e); } });
        run("F. store04 malformed, try-with-resources", fs -> { try { return Reports.tryWithResources(fs); } catch (IOException e) { throw new RuntimeException(e); } });
        System.out.println("G. suppressed exception demo:");
        SuppressedDemo.main(new String[0]);
    }
}
