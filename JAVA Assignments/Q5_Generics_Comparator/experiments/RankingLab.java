import java.util.*;

/** Q5: ranking principle tests. Correct comparator, faulty comparators, property checks, timing. */
public class RankingLab {

    static final class Team {
        final String name; final int points; final int goalDiff;
        Team(String name, int points, int goalDiff) { this.name = name; this.points = points; this.goalDiff = goalDiff; }
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
    }

    static final class Player {
        final String name; final int goals;
        Player(String name, int goals) { this.name = name; this.goals = goals; }
        @Override public String toString() { return name + "(" + goals + ")"; }
    }

    /** Generic leaderboard: works for any type T with a comparator for T. */
    static class Leaderboard<T> {
        private final List<T> items = new ArrayList<>();
        private final Comparator<? super T> order;
        Leaderboard(Comparator<? super T> order) { this.order = order; }
        void add(T item) { items.add(item); }
        void addAll(List<? extends T> more) { items.addAll(more); }   // wildcard: accepts subtypes of T
        List<T> ranking() { List<T> copy = new ArrayList<>(items); copy.sort(order); return copy; }
        int size() { return items.size(); }
    }

    /** The principle-based comparator: points desc, goal difference desc, name asc. */
    static final Comparator<Team> BY_RANK = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    /** Faulty 1: subtraction, which overflows for large values. */
    static final Comparator<Team> SUBTRACT = (a, b) -> (b.points - a.points) != 0 ? (b.points - a.points)
            : (b.goalDiff - a.goalDiff) != 0 ? (b.goalDiff - a.goalDiff) : a.name.compareTo(b.name);
    /** Faulty 2: ignores the tie-breakers. */
    static final Comparator<Team> POINTS_ONLY = Comparator.comparingInt((Team t) -> t.points).reversed();
    /** Faulty 3: random result for ties (not a function of the items). */
    static Comparator<Team> randomTies(Random r) {
        return (a, b) -> a.points != b.points ? Integer.compare(b.points, a.points) : r.nextInt(3) - 1;
    }

    static int sign(int x) { return Integer.signum(x); }

    static String checkProperties(Comparator<Team> c, List<Team> pool, int trials, Random r) {
        int antisym = 0, trans = 0, equalWrong = 0;
        for (int t = 0; t < trials; t++) {
            Team a = pool.get(r.nextInt(pool.size())), b = pool.get(r.nextInt(pool.size())), d = pool.get(r.nextInt(pool.size()));
            if (sign(c.compare(a, b)) != -sign(c.compare(b, a))) antisym++;
            if (c.compare(a, b) < 0 && c.compare(b, d) < 0 && c.compare(a, d) >= 0) trans++;
            boolean sameKey = a.points == b.points && a.goalDiff == b.goalDiff && a.name.equals(b.name);
            if ((c.compare(a, b) == 0) != sameKey) equalWrong++;
        }
        return String.format("antisymmetry violations=%d, transitivity violations=%d, equal-but-not-equal or distinct-but-zero=%d (of %d trials)",
                antisym, trans, equalWrong, trials);
    }

    static double medianMs(Runnable r, int reps) {
        r.run();
        double[] t = new double[reps];
        for (int k = 0; k < reps; k++) { long t0 = System.nanoTime(); r.run(); t[k] = (System.nanoTime() - t0) / 1e6; }
        Arrays.sort(t);
        return t[reps / 2];
    }

    public static void main(String[] args) {
        System.out.println("== A. Tie-heavy data set (hand-worked expected order) ==");
        List<Team> ties = List.of(new Team("Panthers", 9, 4), new Team("Eagles", 9, 6), new Team("Cobras", 6, 1),
                new Team("Bisons", 9, 4), new Team("Dolphins", 9, 6), new Team("Wolves", 9, 2), new Team("Ants", 6, 1),
                new Team("Owls", 6, 1), new Team("Zebras", 0, -3));
        String expected = "[Dolphins(9,6), Eagles(9,6), Bisons(9,4), Panthers(9,4), Wolves(9,2), Ants(6,1), Cobras(6,1), Owls(6,1), Zebras(0,-3)]";
        Leaderboard<Team> board = new Leaderboard<>(BY_RANK);
        board.addAll(ties);
        String got = board.ranking().toString();
        System.out.println("  expected " + expected);
        System.out.println("  observed " + got + "  -> " + (expected.equals(got) ? "MATCH" : "DIFFERENT"));

        System.out.println("== B. Faulty comparators on the tie data ==");
        for (Map.Entry<String, Comparator<Team>> e : new LinkedHashMap<String, Comparator<Team>>() {{
                put("subtraction", SUBTRACT); put("points only", POINTS_ONLY); }}.entrySet()) {
            Leaderboard<Team> b = new Leaderboard<>(e.getValue());
            b.addAll(ties);
            String g = b.ranking().toString();
            System.out.println("  " + e.getKey() + ": " + g + "  -> " + (expected.equals(g) ? "same as expected" : "WRONG"));
        }
        System.out.println("  subtraction overflow: compare(Team(MIN+1 points), Team(MAX points)) = "
                + SUBTRACT.compare(new Team("low", Integer.MIN_VALUE + 1, 0), new Team("high", Integer.MAX_VALUE, 0))
                + " (correct answer: low ranks below high, so the value should be positive)");

        System.out.println("== C. Contract violations on random triples (5 pools of 200 teams with ties, 50,000 triples each) ==");
        Random pr = new Random(5);
        List<Team> pool = new ArrayList<>();
        for (int i = 0; i < 200; i++) pool.add(new Team("T" + (i % 40), pr.nextInt(5), pr.nextInt(5) - 2));
        for (String name : new String[]{"correct BY_RANK", "subtraction", "points only", "random ties"}) {
            Comparator<Team> c = name.equals("correct BY_RANK") ? BY_RANK : name.equals("subtraction") ? SUBTRACT
                    : name.equals("points only") ? POINTS_ONLY : randomTies(new Random(3));
            System.out.println("  " + name + ": " + checkProperties(c, pool, 50_000, new Random(11)));
        }
        System.out.println("  correct BY_RANK on a pool with MIN/MAX points (overflow test): " + checkProperties(BY_RANK,
                List.of(new Team("a", Integer.MIN_VALUE, 0), new Team("b", Integer.MAX_VALUE, 0), new Team("c", 0, 0)), 3000, new Random(2)));

        System.out.println("== D. TimSort with a faulty comparator (random ties), 1000 random lists of 300 teams ==");
        int thrown = 0;
        for (int k = 0; k < 1000; k++) {
            List<Team> list = new ArrayList<>();
            Random rr = new Random(k);
            for (int i = 0; i < 300; i++) list.add(new Team("x" + i, rr.nextInt(3), 0));
            try { list.sort(randomTies(new Random(k + 17))); }
            catch (IllegalArgumentException ex) { thrown++; }
        }
        System.out.println("  IllegalArgumentException 'Comparison method violates its general contract!' thrown in " + thrown + " of 1000 sorts");
        int thrownCorrect = 0;
        for (int k = 0; k < 1000; k++) {
            List<Team> list = new ArrayList<>();
            Random rr = new Random(k);
            for (int i = 0; i < 300; i++) list.add(new Team("x" + i, rr.nextInt(3), rr.nextInt(5)));
            try { list.sort(BY_RANK); } catch (IllegalArgumentException ex) { thrownCorrect++; }
        }
        System.out.println("  with the correct BY_RANK: exception thrown in " + thrownCorrect + " of 1000 sorts");

        System.out.println("== E. Generic reuse: the same Leaderboard<T> for players ==");
        Leaderboard<Player> players = new Leaderboard<>(Comparator.comparingInt((Player p) -> p.goals).reversed().thenComparing(p -> p.name));
        players.add(new Player("Ravi", 7)); players.add(new Player("Asha", 9)); players.add(new Player("Kiran", 7));
        System.out.println("  players ranking: " + players.ranking());

        System.out.println("== F. Time to sort n teams with BY_RANK (median of 7, ms) and the 3-pass stable alternative ==");
        for (int n : new int[]{1_000, 100_000, 1_000_000}) {
            Random g = new Random(77);
            List<Team> base = new ArrayList<>(n);
            for (int i = 0; i < n; i++) base.add(new Team("T" + g.nextInt(n), g.nextInt(30), g.nextInt(60) - 30));
            double single = medianMs(() -> new ArrayList<>(base).sort(BY_RANK), 7);
            double threePass = medianMs(() -> {
                List<Team> l = new ArrayList<>(base);
                l.sort(Comparator.comparing((Team t) -> t.name));
                l.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
                l.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
            }, 7);
            List<Team> a1 = new ArrayList<>(base); a1.sort(BY_RANK);
            List<Team> a2 = new ArrayList<>(base);
            a2.sort(Comparator.comparing((Team t) -> t.name));
            a2.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
            a2.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
            boolean same = true;
            for (int i = 0; i < n; i++) if (a1.get(i) != a2.get(i)) { same = false; break; }
            System.out.printf("  n=%-9d single comparator %.2f ms, three stable passes %.2f ms, same order: %s%n", n, single, threePass, same);
        }
    }
}
