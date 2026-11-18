import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Runs all files on a fixed pool and builds the summary. Results go to a ConcurrentHashMap keyed by file name. */
public class Processor {
    static final class Summary {
        final Map<Outcome.Status, Integer> counts = new EnumMap<>(Outcome.Status.class);
        int files, unhandledFutureFailures; long millis; long totalMarks;
        @Override public String toString() {
            return "files=" + files + " ok=" + counts.getOrDefault(Outcome.Status.OK, 0)
                 + " corrupt=" + counts.getOrDefault(Outcome.Status.CORRUPT, 0)
                 + " missing=" + counts.getOrDefault(Outcome.Status.MISSING, 0)
                 + " internal=" + counts.getOrDefault(Outcome.Status.INTERNAL_ERROR, 0)
                 + " io=" + counts.getOrDefault(Outcome.Status.IO_ERROR, 0)
                 + " unhandled=" + unhandledFutureFailures + " time=" + millis + " ms";
        }
    }

    static Summary run(Path dir, List<String> names, int threads, long ioLatencyMs) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ConcurrentHashMap<String, Outcome> results = new ConcurrentHashMap<>();
        List<Future<Outcome>> futures = new ArrayList<>();
        long t0 = System.nanoTime();
        for (String name : names) futures.add(pool.submit(new ScriptTask(dir.resolve(name), ioLatencyMs)));
        Summary s = new Summary();
        for (int i = 0; i < futures.size(); i++) {
            try {
                Outcome o = futures.get(i).get();
                results.put(o.file, o);
            } catch (ExecutionException e) {                             // should not happen: call() never throws
                s.unhandledFutureFailures++;
            }
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        s.millis = (System.nanoTime() - t0) / 1_000_000;
        s.files = results.size();
        for (Outcome o : results.values()) {
            s.counts.merge(o.status, 1, Integer::sum);
            if (o.status == Outcome.Status.OK) s.totalMarks += o.total;
        }
        return s;
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "handset";
        if (mode.equals("handset")) {
            Path dir = Paths.get("handset");
            Gen.handSet(dir);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < 10; i++) names.add(String.format("h%02d.txt", i));
            Summary s = run(dir, names.subList(0, 10), 4, 0);
            System.out.println("hand-checked set (expected ok=7 corrupt=2 missing=1): " + s);
            return;
        }
        int n = Integer.parseInt(args[1]);
        int threads = Integer.parseInt(args[2]);
        long io = Long.parseLong(args[3]);
        Path dir = Paths.get("scripts");
        if (Files.exists(dir)) {
            try (java.util.stream.Stream<Path> w = Files.walk(dir)) {
                w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        String[] cat = Gen.generate(dir, n, 2024);
        Map<String, Integer> expected = new TreeMap<>();
        for (String c : cat) expected.merge(c, 1, Integer::sum);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) names.add(String.format("s%05d.txt", i));
        Summary s = run(dir, names, threads, io);
        System.out.println("expected by generator: " + expected);
        System.out.println("n=" + n + " threads=" + threads + " io=" + io + " ms: " + s);
    }
}
