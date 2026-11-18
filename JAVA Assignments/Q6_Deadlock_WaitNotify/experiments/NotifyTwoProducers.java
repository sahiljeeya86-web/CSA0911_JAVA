/** Q6: notify() with TWO producers and TWO consumers on a bounded buffer. Counts hangs. */
public class NotifyTwoProducers {
    static class Buffer {
        final int[] b = new int[5]; int count = 0, in = 0, out = 0;
        final boolean all;
        Buffer(boolean all) { this.all = all; }
        synchronized void put(int t) throws InterruptedException {
            while (count == b.length) wait();
            b[in] = t; in = (in + 1) % b.length; count++;
            if (all) notifyAll(); else notify();
        }
        synchronized int take() throws InterruptedException {
            while (count == 0) wait();
            int t = b[out]; out = (out + 1) % b.length; count--;
            if (all) notifyAll(); else notify();
            return t;
        }
    }

    static boolean trial(boolean all) throws InterruptedException {
        Buffer buf = new Buffer(all);
        Thread[] threads = new Thread[4];
        for (int p = 0; p < 2; p++) {
            threads[p] = new Thread(() -> { try { for (int i = 0; i < 200; i++) buf.put(1); } catch (InterruptedException e) { } });
        }
        for (int c = 0; c < 2; c++) {
            threads[2 + c] = new Thread(() -> { try { for (int i = 0; i < 200; i++) buf.take(); } catch (InterruptedException e) { } });
        }
        for (Thread t : threads) t.start();
        boolean hung = false;
        for (Thread t : threads) { t.join(200); if (t.isAlive()) hung = true; }
        for (Thread t : threads) t.interrupt();
        Thread.sleep(20);
        return hung;
    }

    public static void main(String[] args) throws InterruptedException {
        for (boolean all : new boolean[]{true, false}) {
            int hangs = 0, trials = 300;
            for (int t = 0; t < trials; t++) if (trial(all)) hangs++;
            System.out.println((all ? "notifyAll" : "notify   ") + ": " + hangs + " of " + trials + " trials hung (threads still waiting after 200 ms)");
        }
    }
}
