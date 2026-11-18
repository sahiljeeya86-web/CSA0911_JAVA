import java.util.*;

/** Q6: triage with PriorityQueue: hand-checked instance, growth, starvation and an aging rule, alternatives. */
public class TriageLab {
    static final class Patient {
        final String name; final int severity; final long arrival;
        Patient(String name, int severity, long arrival) { this.name = name; this.severity = severity; this.arrival = arrival; }
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }
    static final Comparator<Patient> ORDER = Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival);
    static long sink = 0;

    public static void main(String[] args) {
        System.out.println("== A. Hand-checked instance (8 patients) ==");
        String[] names = {"A", "B", "C", "D", "E", "F", "G", "H"};
        int[] sev = {3, 1, 5, 3, 2, 1, 4, 5};
        PriorityQueue<Patient> q = new PriorityQueue<>(ORDER);
        for (int i = 0; i < names.length; i++) q.add(new Patient(names[i], sev[i], i));
        System.out.println("  heap array as printed by toString (NOT treatment order): " + q);
        StringBuilder order = new StringBuilder();
        for (Patient p; (p = q.poll()) != null; ) order.append(p).append(' ');
        String expected = "B(S1) F(S1) E(S2) A(S3) D(S3) G(S4) C(S5) H(S5)";
        System.out.println("  served order: " + order.toString().trim());
        System.out.println("  hand-worked : " + expected + "  -> " + (order.toString().trim().equals(expected) ? "MATCH" : "DIFFERENT"));

        System.out.println("== B. Growth: ns per operation (median of 5 batches of 1,000 operations) ==");
        System.out.printf("  %-9s %-14s %-14s %-22s%n", "queue n", "admit (add)", "serve (poll)", "sorted LinkedList insert");
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            Random r = new Random(4);
            double[] addT = new double[5], pollT = new double[5], linT = new double[5];
            for (int rep = 0; rep < 6; rep++) {
                PriorityQueue<Patient> pq = new PriorityQueue<>(ORDER);
                for (int i = 0; i < n; i++) pq.add(new Patient("p", 1 + r.nextInt(5), i));
                long t0 = System.nanoTime();
                for (int i = 0; i < 1000; i++) pq.add(new Patient("n", 1 + r.nextInt(5), n + i));
                long t1 = System.nanoTime();
                for (int i = 0; i < 1000; i++) sink += pq.poll().arrival;
                long t2 = System.nanoTime();
                LinkedList<Patient> sorted = new LinkedList<>();
                for (int i = 0; i < n; i++) sorted.add(new Patient("p", 1 + r.nextInt(5), i));
                sorted.sort(ORDER);
                long t3 = System.nanoTime();
                for (int i = 0; i < 1000; i++) {
                    Patient np = new Patient("n", 1 + r.nextInt(5), n + i);
                    int pos = 0;
                    ListIterator<Patient> it = sorted.listIterator();
                    while (it.hasNext() && ORDER.compare(it.next(), np) <= 0) pos++;
                    sorted.add(pos, np);
                }
                long t4 = System.nanoTime();
                if (rep > 0) { addT[rep - 1] = (t1 - t0) / 1000.0; pollT[rep - 1] = (t2 - t1) / 1000.0; linT[rep - 1] = (t4 - t3) / 1000.0; }
            }
            Arrays.sort(addT); Arrays.sort(pollT); Arrays.sort(linT);
            System.out.printf("  %-9d %-14.1f %-14.1f %-22.1f%n", n, addT[2], pollT[2], linT[2]);
        }

        System.out.println("== C. Starvation: severity-5 patient under a steady stream (1 arrival and 1 service per tick) ==");
        System.out.println("  arrivals: 2 per tick, 90% severity 1-3 and 10% severity 5; one service per tick; 2,000 ticks (overloaded)");
        for (int ageTicks : new int[]{0, 20, 50}) {
            Random r = new Random(21);
            long maxWaitS5 = 0, firstS5Served = -1; int served5 = 0, waiting5max = 0;
            List<Patient> waiting = new ArrayList<>();
            long id = 0;
            for (long tick = 0; tick < 2000; tick++) {
                for (int arr = 0; arr < 2; arr++) {                 // two arrivals per tick, one service per tick
                    int s = r.nextDouble() < 0.9 ? 1 + r.nextInt(3) : 5;
                    waiting.add(new Patient("p" + id, s, tick));
                    id++;
                }
                // effective severity: aged by waiting time when ageTicks > 0 (rule: one level per ageTicks waited)
                final long now = tick;
                final int age = ageTicks;
                Comparator<Patient> eff = (a, b) -> {
                    int ea = effective(a, now, age), eb = effective(b, now, age);
                    if (ea != eb) return Integer.compare(ea, eb);
                    return Long.compare(a.arrival, b.arrival);
                };
                PriorityQueue<Patient> pq = new PriorityQueue<>(eff);
                pq.addAll(waiting);
                Patient served = pq.poll();
                waiting.remove(served);
                if (served.severity == 5) {
                    served5++;
                    if (firstS5Served < 0) firstS5Served = tick;
                    maxWaitS5 = Math.max(maxWaitS5, tick - served.arrival);
                }
                for (Patient p : waiting) if (p.severity == 5) waiting5max = Math.max(waiting5max, (int) (tick - p.arrival));
            }
            System.out.printf("  aging every %2s ticks: severity-5 served %d times; longest wait served %d ticks; oldest still waiting at end %d ticks%n",
                    ageTicks == 0 ? "never" : String.valueOf(ageTicks), served5, maxWaitS5, waiting5max);
        }

        System.out.println("== D. Deployment check: 5,000 patients a day ==");
        Random r = new Random(8);
        long t0 = System.nanoTime();
        PriorityQueue<Patient> day = new PriorityQueue<>(ORDER);
        long served = 0;
        for (int i = 0; i < 5000; i++) {
            day.add(new Patient("d" + i, 1 + r.nextInt(5), i));
            if (i % 3 == 2) { sink += day.poll().arrival; served++; }
        }
        while (!day.isEmpty()) { sink += day.poll().arrival; served++; }
        long dt = System.nanoTime() - t0;
        System.out.printf("  5,000 admissions and %d services in %.3f ms in total (%.0f ns per patient); average queue length well under 5,000%n",
                served, dt / 1e6, dt / 5000.0);
        System.out.println("sink=" + sink);
    }

    static int effective(Patient p, long now, int ageTicks) {
        if (ageTicks <= 0) return p.severity;
        long waited = now - p.arrival;
        return Math.max(1, p.severity - (int) (waited / ageTicks));
    }
}
