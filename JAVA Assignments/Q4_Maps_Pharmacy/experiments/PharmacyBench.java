import java.util.*;

/** Q4: HashMap, TreeMap, LinkedHashMap for a pharmacy stock table, and a sorted array alternative. */
public class PharmacyBench {
    record Stock(String code, String name, int qty) {}
    static long sink = 0;

    static String code(int i) { return String.format("MED%07d", i); }

    static String[] makeCodes(int n, long seed) {
        // distinct codes, inserted in a shuffled order so insertion order differs from code order
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = i;
        Random r = new Random(seed);
        for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = ids[i]; ids[i] = ids[j]; ids[j] = t; }
        String[] out = new String[n];
        for (int i = 0; i < n; i++) out[i] = code(ids[i]);
        return out;
    }

    static double medianNs(Runnable r, int reps) {
        r.run();
        double[] t = new double[5];
        for (int k = 0; k < 5; k++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < reps; i++) r.run();
            t[k] = (System.nanoTime() - t0) / (double) reps;
        }
        Arrays.sort(t);
        return t[2];
    }

    public static void main(String[] args) {
        int[] sizes = {1_000, 100_000, 1_000_000};
        for (int n : sizes) {
            String[] codes = makeCodes(n, 42);
            Random r = new Random(9);
            String[] probes = new String[100_000];
            for (int i = 0; i < probes.length; i++) probes[i] = codes[r.nextInt(n)];

            Map<String, Stock> hash = new HashMap<>(), tree = new TreeMap<>(), linked = new LinkedHashMap<>();
            long tPut0 = System.nanoTime();
            for (String c : codes) hash.put(c, new Stock(c, "drug", 10));
            long putHash = System.nanoTime() - tPut0;
            tPut0 = System.nanoTime();
            for (String c : codes) tree.put(c, new Stock(c, "drug", 10));
            long putTree = System.nanoTime() - tPut0;
            tPut0 = System.nanoTime();
            for (String c : codes) linked.put(c, new Stock(c, "drug", 10));
            long putLinked = System.nanoTime() - tPut0;

            System.out.println("== n = " + n + " medicines; 100,000 random lookups; reports ==");
            System.out.printf("  put all (ms): HashMap %.1f, TreeMap %.1f, LinkedHashMap %.1f%n",
                    putHash / 1e6, putTree / 1e6, putLinked / 1e6);

            double lh = medianNs(() -> { long s = 0; for (String p : probes) s += hash.get(p).qty(); sink += s; }, 3) / probes.length;
            double lt = medianNs(() -> { long s = 0; for (String p : probes) s += tree.get(p).qty(); sink += s; }, 3) / probes.length;
            double ll = medianNs(() -> { long s = 0; for (String p : probes) s += linked.get(p).qty(); sink += s; }, 3) / probes.length;
            System.out.printf("  100,000 lookups total (ms): HashMap %.2f (%.1f ns each), TreeMap %.2f (%.1f ns each), LinkedHashMap %.2f (%.1f ns each)%n",
                    lh * probes.length / 1e6, lh, lt * probes.length / 1e6, lt, ll * probes.length / 1e6, ll);

            // code-order report
            double tree_rep = medianNs(() -> { StringBuilder b = new StringBuilder(); for (Stock s : tree.values()) b.append(s.code()).append('\n'); sink += b.length(); }, 2);
            double hash_rep = medianNs(() -> {
                List<String> keys = new ArrayList<>(hash.keySet());
                Collections.sort(keys);
                StringBuilder b = new StringBuilder();
                for (String k : keys) b.append(hash.get(k).code()).append('\n');
                sink += b.length();
            }, 2);
            double linked_rep = medianNs(() -> { StringBuilder b = new StringBuilder(); for (Stock s : linked.values()) b.append(s.code()).append('\n'); sink += b.length(); }, 2);
            System.out.printf("  code-order report (ms): TreeMap %.2f (natural order), HashMap sort-then-print %.2f, LinkedHashMap %.2f (wrong order)%n",
                    tree_rep / 1e6, hash_rep / 1e6, linked_rep / 1e6);
            System.out.printf("  insertion-order report (ms): LinkedHashMap %.2f (natural), HashMap %.2f (no order), TreeMap %.2f (wrong order)%n",
                    medianNs(() -> { StringBuilder b = new StringBuilder(); for (Stock s : linked.values()) b.append(s.code()).append('\n'); sink += b.length(); }, 2) / 1e6,
                    medianNs(() -> { StringBuilder b = new StringBuilder(); for (Stock s : hash.values()) b.append(s.code()).append('\n'); sink += b.length(); }, 2) / 1e6,
                    medianNs(() -> { StringBuilder b = new StringBuilder(); for (Stock s : tree.values()) b.append(s.code()).append('\n'); sink += b.length(); }, 2) / 1e6);

            // order checks
            List<String> treeOrder = new ArrayList<>(tree.keySet());
            List<String> linkedOrder = new ArrayList<>(linked.keySet());
            System.out.println("  TreeMap keys sorted: " + isSorted(treeOrder) + ";  LinkedHashMap keys match insertion order: " + linkedOrder.equals(Arrays.asList(codes)));

            // Hashtable for comparison
            Hashtable<String, Stock> table = new Hashtable<>();
            for (String c : codes) table.put(c, new Stock(c, "drug", 10));
            double lht = medianNs(() -> { long s = 0; for (String p : probes) s += table.get(p).qty(); sink += s; }, 3) / probes.length;
            System.out.printf("  Hashtable 100,000 lookups total (ms): %.2f (%.1f ns each)%n", lht * probes.length / 1e6, lht);

            // sorted array alternative: binarySearch on Stock[] sorted by code
            Stock[] arr = new Stock[n];
            int k = 0;
            for (Stock s : tree.values()) arr[k++] = s;
            double lb = medianNs(() -> {
                long s = 0;
                for (String p : probes) {
                    int idx = Arrays.binarySearch(arr, new Stock(p, "", 0), Comparator.comparing(Stock::code));
                    s += arr[idx].qty();
                }
                sink += s;
            }, 2) / probes.length;
            System.out.printf("  sorted array + binarySearch, 100,000 lookups total (ms): %.2f (%.1f ns each)%n%n", lb * probes.length / 1e6, lb);
        }
        System.out.println("sink=" + sink);
    }

    static boolean isSorted(List<String> l) {
        for (int i = 1; i < l.size(); i++) if (l.get(i - 1).compareTo(l.get(i)) > 0) return false;
        return true;
    }
}
