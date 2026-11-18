import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/** Q7 alternatives: parallelStream and one thread per file, on the same 10,000 files (1 ms simulated I/O). */
public class AltStreams {
    static String name(int i) { return String.format("s%05d.txt", i); }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path dir = Paths.get("../scripts");
        int n = 10000;
        List<String> names = IntStream.range(0, n).mapToObj(AltStreams::name).collect(Collectors.toList());
        long t0 = System.nanoTime();
        int ok = 0, other = 0, failed = 0;
        if (mode.equals("parallelStream")) {
            // parallel streams use the common ForkJoinPool, sized to the number of cores (here 1)
            List<Outcome> out = names.parallelStream().map(nm -> new ScriptTask(dir.resolve(nm), 1).call()).collect(Collectors.toList());
            for (Outcome o : out) { if (o.status == Outcome.Status.OK) ok++; else other++; }
        } else if (mode.equals("threadPerFile")) {
            List<Thread> ts = new ArrayList<>();
            ConcurrentLinkedQueue<Outcome> out = new ConcurrentLinkedQueue<>();
            try {
                for (String nm : names) {
                    Thread t = new Thread(() -> out.add(new ScriptTask(dir.resolve(nm), 1).call()));
                    ts.add(t);
                    t.start();
                }
                for (Thread t : ts) t.join();
            } catch (OutOfMemoryError e) {
                failed++;
                System.out.println("OutOfMemoryError after creating " + ts.size() + " threads: " + e.getMessage());
            }
            for (Outcome o : out) { if (o.status == Outcome.Status.OK) ok++; else other++; }
            System.out.println("threads created: " + ts.size());
        } else {
            ExecutorService pool = Executors.newFixedThreadPool(Integer.parseInt(mode));
            List<Future<Outcome>> fs = new ArrayList<>();
            for (String nm : names) fs.add(pool.submit(new ScriptTask(dir.resolve(nm), 1)));
            for (Future<Outcome> f : fs) { Outcome o = f.get(); if (o.status == Outcome.Status.OK) ok++; else other++; }
            pool.shutdown();
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println(mode + ": ok=" + ok + " other=" + other + " time=" + ms + " ms" + (failed > 0 ? " (run aborted)" : ""));
        System.exit(0);
    }
}
