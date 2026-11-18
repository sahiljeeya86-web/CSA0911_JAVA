import java.util.*;

/** Q7: original Leaderboard (sort on every ranking request) versus SortedLeaderboard (kept in order). */
public class RedesignLab {
    /** Immutable team, so a sorted position stays valid after insertion (no mutable-key problem). */
    static final class Team {
        final String name; final int points; final int goalDiff;
        Team(String name, int points, int goalDiff) { this.name = name; this.points = points; this.goalDiff = goalDiff; }
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
    }

    static long comparisons = 0;
    static Comparator<Team> counting(Comparator<Team> base) {
        return (a, b) -> { comparisons++; return base.compare(a, b); };
    }
    static final Comparator<Team> RANK = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    /** Original design from Q5: keeps items in arrival order and sorts a copy on every request. */
    static class Leaderboard {
        final List<Team> items = new ArrayList<>();
        final Comparator<Team> order;
        Leaderboard(Comparator<Team> order) { this.order = order; }
        void add(Team t) { items.add(t); }
        List<Team> ranking() { List<Team> copy = new ArrayList<>(items); copy.sort(order); return copy; }
        List<Team> top(int k) { return ranking().subList(0, Math.min(k, items.size())); }
    }

    /** Redesign: the list is kept in ranking order; insertion uses binary search. */
    static class SortedLeaderboard {
        final List<Team> items = new ArrayList<>();
        final Comparator<Team> order;
        SortedLeaderboard(Comparator<Team> order) { this.order = order; }
        void add(Team t) {
            int pos = Collections.binarySearch(items, t, order);
            if (pos < 0) pos = -pos - 1;            // insertion point; equal items go after existing equals
            items.add(pos, t);
        }
        List<Team> ranking() { return new ArrayList<>(items); }
        List<Team> top(int k) { return new ArrayList<>(items.subList(0, Math.min(k, items.size()))); }
    }

    static Team randomTeam(Random r, int i) { return new Team("T" + i, r.nextInt(40), r.nextInt(80) - 40); }

    public static void main(String[] args) {
        int[] sizes = {10_000, 100_000};
        int[] requests = {2_000, 200};
        for (int si = 0; si < sizes.length; si++) {
            int n = sizes[si], req = requests[si];
            Random r = new Random(31);
            List<Team> initial = new ArrayList<>();
            for (int i = 0; i < n; i++) initial.add(randomTeam(r, i));
            List<Team> incoming = new ArrayList<>();
            for (int i = 0; i < req; i++) incoming.add(randomTeam(new Random(1000 + i), n + i));

            System.out.println("== n = " + n + " teams; " + req + " requests of (add one team, then top 10) ==");
            // original
            Leaderboard orig = new Leaderboard(counting(RANK));
            for (Team t : initial) orig.items.add(t);
            comparisons = 0;
            List<List<Team>> origTops = new ArrayList<>();
            long t0 = System.nanoTime();
            for (int i = 0; i < req; i++) { orig.add(incoming.get(i)); origTops.add(orig.top(10)); }
            long origNs = System.nanoTime() - t0;
            long origCmp = comparisons;
            // redesign
            SortedLeaderboard sl = new SortedLeaderboard(counting(RANK));
            for (Team t : initial) sl.items.add(t);
            List<Team> sorted0 = new ArrayList<>(sl.items);
            sorted0.sort(RANK);
            sl.items.clear(); sl.items.addAll(sorted0);
            comparisons = 0;
            List<List<Team>> newTops = new ArrayList<>();
            t0 = System.nanoTime();
            for (int i = 0; i < req; i++) { sl.add(incoming.get(i)); newTops.add(sl.top(10)); }
            long newNs = System.nanoTime() - t0;
            long newCmp = comparisons;
            boolean same = origTops.equals(newTops);
            System.out.printf("  original : %.1f us per request, %.0f comparisons per request%n", origNs / 1e3 / req, origCmp / (double) req);
            System.out.printf("  redesign : %.1f us per request, %.1f comparisons per request%n", newNs / 1e3 / req, newCmp / (double) req);
            System.out.printf("  speed-up of the request: %.1fx; top-10 lists identical in all %d requests: %s%n",
                    origNs / (double) newNs, req, same);

            // full ranking request, no update
            long fullOrig = bestOf(() -> orig.ranking());
            long fullNew = bestOf(() -> sl.ranking());
            System.out.printf("  full ranking() request, no update: original %.2f ms, redesign %.2f ms (%.1fx)%n%n",
                    fullOrig / 1e6, fullNew / 1e6, fullOrig / (double) fullNew);
        }
        System.out.println("Memory: the original stores n references; the redesign stores n references in sorted order (about the same).");
    }

    static long bestOf(Runnable r) {
        r.run();
        long best = Long.MAX_VALUE;
        for (int k = 0; k < 5; k++) { long t0 = System.nanoTime(); r.run(); best = Math.min(best, System.nanoTime() - t0); }
        return best;
    }
}
