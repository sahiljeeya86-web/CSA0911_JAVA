import java.util.*;

/** Q2: ArrayList versus LinkedList for playlist operations, plus the ArrayDeque 'Up Next' queue. */
public class ListBench {
    static long sink = 0;
    static final int M = 1000;                   // operations timed per measurement

    interface Op { void run(List<Integer> l, int n, Random r); }

    static double medianOpNs(Supplier0 make, Op op, int n) {
        Random r = new Random(7);
        double[] t = new double[5];
        for (int rep = 0; rep < 6; rep++) {
            List<Integer> l = make.get(n);
            long t0 = System.nanoTime();
            op.run(l, n, r);
            long dt = System.nanoTime() - t0;
            if (rep > 0) t[rep - 1] = dt / (double) M;   // first repetition is warm-up
        }
        Arrays.sort(t);
        return t[2];
    }
    interface Supplier0 { List<Integer> get(int n); }

    static List<Integer> fill(List<Integer> l, int n) { for (int i = 0; i < n; i++) l.add(i); return l; }

    public static void main(String[] args) {
        int[] sizes = {1_000, 10_000, 100_000};
        String[] names = {"ArrayList", "LinkedList"};
        Map<String, Supplier0> makers = new LinkedHashMap<>();
        makers.put("ArrayList", n -> fill(new ArrayList<>(), n + M));
        makers.put("LinkedList", n -> fill(new LinkedList<>(), n + M));

        Map<String, Op> ops = new LinkedHashMap<>();
        ops.put("add at end", (l, n, r) -> { for (int i = 0; i < M; i++) l.add(i); sink += l.size(); });
        ops.put("insert at front", (l, n, r) -> { for (int i = 0; i < M; i++) l.add(0, i); sink += l.size(); });
        ops.put("insert in middle", (l, n, r) -> { for (int i = 0; i < M; i++) l.add(l.size() / 2, i); sink += l.size(); });
        ops.put("remove at front", (l, n, r) -> { for (int i = 0; i < M; i++) l.remove(0); sink += l.size(); });
        ops.put("remove in middle", (l, n, r) -> { for (int i = 0; i < M; i++) l.remove(l.size() / 2); sink += l.size(); });
        ops.put("get k-th (random k)", (l, n, r) -> { for (int i = 0; i < M; i++) sink += l.get(r.nextInt(l.size())); });
        ops.put("iterate all (per element)", (l, n, r) -> { long s = 0; for (int i = 0; i < 1; i++) for (int v : l) s += v; sink += s; });

        for (int n : sizes) {
            System.out.println("== playlist size n = " + n + " (ns per operation, median of 5) ==");
            System.out.printf("  %-24s %16s %16s%n", "operation", "ArrayList", "LinkedList");
            for (Map.Entry<String, Op> e : ops.entrySet()) {
                double a = medianOpNs(makers.get("ArrayList"), e.getValue(), n);
                double b = medianOpNs(makers.get("LinkedList"), e.getValue(), n);
                if (e.getKey().startsWith("iterate")) {
                    // iteration: time one full pass over n elements divided by n
                    double ai = iterNs(makers.get("ArrayList").get(n));
                    double bi = iterNs(makers.get("LinkedList").get(n));
                    System.out.printf("  %-24s %16.2f %16.2f%n", e.getKey(), ai, bi);
                } else {
                    System.out.printf("  %-24s %16.1f %16.1f%n", e.getKey(), a, b);
                }
            }
            System.out.println();
        }

        // Up Next queue: add at the end, remove from the front, steady size
        System.out.println("== 'Up Next' queue: 200,000 add-end + remove-front pairs, steady size 1,000 (ns per pair) ==");
        for (String kind : new String[]{"ArrayList", "LinkedList", "ArrayDeque"}) {
            double[] t = new double[5];
            for (int rep = 0; rep < 6; rep++) {
                long t0 = System.nanoTime();
                if (kind.equals("ArrayDeque")) {
                    Deque<Integer> d = new ArrayDeque<>();
                    for (int i = 0; i < 1000; i++) d.addLast(i);
                    for (int i = 0; i < 200_000; i++) { d.addLast(i); sink += d.removeFirst(); }
                } else {
                    List<Integer> l = kind.equals("ArrayList") ? new ArrayList<>() : new LinkedList<>();
                    for (int i = 0; i < 1000; i++) l.add(i);
                    for (int i = 0; i < 200_000; i++) { l.add(i); sink += l.remove(0); }
                }
                long dt = System.nanoTime() - t0;
                if (rep > 0) t[rep - 1] = dt / 200_000.0;
            }
            Arrays.sort(t);
            System.out.printf("  %-12s %10.1f ns per add+remove pair%n", kind, t[2]);
        }
        System.out.println("sink=" + sink);
    }

    static double iterNs(List<Integer> l) {
        double[] t = new double[5];
        for (int rep = 0; rep < 6; rep++) {
            long t0 = System.nanoTime();
            long s = 0;
            for (int v : l) s += v;
            sink += s;
            long dt = System.nanoTime() - t0;
            if (rep > 0) t[rep - 1] = dt / (double) l.size();
        }
        Arrays.sort(t);
        return t[2];
    }
}
