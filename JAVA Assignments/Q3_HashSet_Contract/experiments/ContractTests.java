import java.util.*;

/** Q3 experiments: equals/hashCode contract, mutation after insertion, inconsistent compareTo. */
public class ContractTests {

    /** The junior developer's class: equals() without hashCode(). */
    static class BadStudent {
        final String regNo, name;
        BadStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object o) {
            return o instanceof BadStudent && regNo.equals(((BadStudent) o).regNo);
        }
    }

    /** Corrected class: equals() and hashCode() both use regNo. */
    static class GoodStudent {
        final String regNo, name;
        GoodStudent(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object o) {
            return o instanceof GoodStudent && regNo.equals(((GoodStudent) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    /** Mutable key: regNo can change after the object is inserted. */
    static class MutableStudent {
        String regNo;
        MutableStudent(String regNo) { this.regNo = regNo; }
        @Override public boolean equals(Object o) {
            return o instanceof MutableStudent && regNo.equals(((MutableStudent) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    /** Equal by regNo, but compareTo uses only the name (inconsistent with equals). */
    static class NameOnly {
        final String regNo, name;
        NameOnly(String regNo, String name) { this.regNo = regNo; this.name = name; }
        @Override public boolean equals(Object o) {
            return o instanceof NameOnly && regNo.equals(((NameOnly) o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }

    static String reg(int i) { return "19231" + String.format("%04d", i); }

    public static void main(String[] args) {
        System.out.println("TEST 1 - one object per student, each scanned once");
        Set<BadStudent> s1 = new HashSet<>();
        Set<GoodStudent> s1g = new HashSet<>();
        for (int i = 0; i < 1000; i++) { s1.add(new BadStudent(reg(i), "S" + i)); s1g.add(new GoodStudent(reg(i), "S" + i)); }
        System.out.println("  predicted 1000 (both)   bad class size=" + s1.size() + "   good class size=" + s1g.size());

        System.out.println("TEST 2 - 1000 students, each scanned again as a NEW object (duplicate scans)");
        Set<BadStudent> s2 = new HashSet<>();
        Set<GoodStudent> s2g = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            s2.add(new BadStudent(reg(i), "S" + i)); s2.add(new BadStudent(reg(i), "S" + i));
            s2g.add(new GoodStudent(reg(i), "S" + i)); s2g.add(new GoodStudent(reg(i), "S" + i));
        }
        System.out.println("  predicted 2000 bad, 1000 good   bad class size=" + s2.size() + "   good class size=" + s2g.size());

        System.out.println("TEST 3 - change regNo after insertion (good class with hashCode)");
        MutableStudent m = new MutableStudent("192310001");
        Set<MutableStudent> s3 = new HashSet<>();
        s3.add(m);
        m.regNo = "192310999";
        System.out.println("  predicted contains(original value)=false, size=1");
        System.out.println("  contains(object)=" + s3.contains(m) + "   size=" + s3.size()
                + "   remove(object)=" + s3.remove(m) + "   size after remove=" + s3.size());
        Set<MutableStudent> fresh = new HashSet<>();
        fresh.add(new MutableStudent("192310999"));
        System.out.println("  a new equal object is found: " + fresh.contains(new MutableStudent("192310999")));

        System.out.println("TEST 4 - TreeSet with compareTo on name only (equal names, different regNo)");
        Set<NameOnly> s4 = new TreeSet<>(Comparator.comparing(x -> x.name));
        s4.add(new NameOnly("192311001", "Arun"));
        s4.add(new NameOnly("192311555", "Arun"));
        System.out.println("  predicted size=1   observed size=" + s4.size());
        Set<NameOnly> s4h = new HashSet<>();
        s4h.add(new NameOnly("192311001", "Arun"));
        s4h.add(new NameOnly("192311555", "Arun"));
        System.out.println("  same two students in a HashSet: size=" + s4h.size());

        System.out.println("TIMING - cost of one add with a good key (average over repeated runs)");
        for (int n : new int[]{1_000, 100_000, 1_000_000}) {
            double hs = medianNs(() -> {
                Set<GoodStudent> h = new HashSet<>();
                for (int i = 0; i < n; i++) h.add(new GoodStudent(reg(i % 500_000), "x"));
            }, 3) / n;
            double ts = medianNs(() -> {
                Set<GoodStudent> t = new TreeSet<>(Comparator.comparing(x -> x.regNo));
                for (int i = 0; i < n; i++) t.add(new GoodStudent(reg(i % 500_000), "x"));
            }, 3) / n;
            System.out.printf("  n=%-9d HashSet %.1f ns/add   TreeSet(regNo) %.1f ns/add%n", n, hs, ts);
        }
    }

    static double medianNs(Runnable r, int reps) {
        r.run();
        double[] t = new double[reps];
        for (int k = 0; k < reps; k++) {
            long t0 = System.nanoTime();
            r.run();
            t[k] = System.nanoTime() - t0;
        }
        Arrays.sort(t);
        return t[reps / 2];
    }
}
