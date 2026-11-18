import java.util.*;

/** Q6: the token counter with if versus while, and notify versus notifyAll, over many trials. */
public class WaitVariants {
    static class Counter {
        final int[] buffer = new int[5];
        int count = 0, in = 0, out = 0;
        int minCount = Integer.MAX_VALUE, maxCount = Integer.MIN_VALUE;   // invariant monitor
        final boolean useWhile, useNotifyAll;
        Counter(boolean useWhile, boolean useNotifyAll) { this.useWhile = useWhile; this.useNotifyAll = useNotifyAll; }

        synchronized void put(int token) throws InterruptedException {
            if (useWhile) { while (count == buffer.length) wait(); } else { if (count == buffer.length) wait(); }
            buffer[in] = token;
            in = (in + 1) % buffer.length;
            count++;
            record();
            if (useNotifyAll) notifyAll(); else notify();
        }

        synchronized int take() throws InterruptedException {
            if (useWhile) { while (count == 0) wait(); } else { if (count == 0) wait(); }
            int token = buffer[out];
            out = (out + 1) % buffer.length;
            count--;
            record();
            if (useNotifyAll) notifyAll(); else notify();
            return token;
        }
        void record() { minCount = Math.min(minCount, count); maxCount = Math.max(maxCount, count); }
    }

    static String trial(boolean useWhile, boolean useNotifyAll, int[] stats) throws InterruptedException {
        Counter c = new Counter(useWhile, useNotifyAll);
        int[] collected = new int[3];
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= 20; t++) c.put(t); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        Thread[] students = new Thread[3];
        final int[] errors = new int[1];
        for (int s = 0; s < 3; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 20 / 3 + (id == 0 ? 2 : 0); k++) collected[id] += c.take(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ArrayIndexOutOfBoundsException e) { errors[0]++; }
            });
        }
        cook.start(); for (Thread t : students) t.start();
        cook.join(500);
        for (Thread t : students) t.join(500);
        boolean hung = cook.isAlive();
        for (Thread t : students) if (t.isAlive()) hung = true;
        if (hung) {
            for (Thread t : students) t.interrupt();
            cook.interrupt();
            Thread.sleep(50);
        }
        int sum = collected[0] + collected[1] + collected[2];
        if (c.minCount < 0 || c.maxCount > 5) stats[2]++;      // invariant broken
        if (hung) return "HUNG";
        if (sum != 210) return "WRONG SUM " + sum;
        return "OK";
    }

    public static void main(String[] args) throws InterruptedException {
        int trials = 200;
        String[][] variants = {{"while", "notifyAll"}, {"if", "notifyAll"}, {"while", "notify"}, {"if", "notify"}};
        for (String[] v : variants) {
            boolean w = v[0].equals("while"), na = v[1].equals("notifyAll");
            Map<String, Integer> outcomes = new TreeMap<>();
            int[] stats = new int[3];
            for (int t = 0; t < trials; t++) {
                String o = trial(w, na, stats);
                outcomes.merge(o, 1, Integer::sum);
            }
            System.out.println(v[0] + " + " + v[1] + ": " + trials + " trials -> " + outcomes
                    + "; trials with count outside 0..5: " + stats[2]);
        }
    }
}
