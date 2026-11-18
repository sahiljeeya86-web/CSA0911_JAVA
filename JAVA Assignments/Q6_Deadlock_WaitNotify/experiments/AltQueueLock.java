import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/** Q6 alternatives: ArrayBlockingQueue versus wait/notifyAll; tryLock with timeout versus ordered locking. */
public class AltQueueLock {
    static final class WaitCounter {
        final int[] b = new int[5]; int count = 0, in = 0, out = 0;
        synchronized void put(int t) throws InterruptedException { while (count == 5) wait(); b[in] = t; in = (in + 1) % 5; count++; notifyAll(); }
        synchronized int take() throws InterruptedException { while (count == 0) wait(); int t = b[out]; out = (out + 1) % 5; count--; notifyAll(); return t; }
    }

    static long wait_(int n) throws InterruptedException {
        WaitCounter c = new WaitCounter();
        Thread p = new Thread(() -> { try { for (int i = 0; i < n; i++) c.put(i); } catch (InterruptedException e) { } });
        long t0 = System.nanoTime();
        p.start();
        long s = 0; for (int i = 0; i < n; i++) s += c.take();
        p.join();
        return (System.nanoTime() - t0) / 1000;
    }

    static long abq(int n) throws InterruptedException {
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
        Thread p = new Thread(() -> { try { for (int i = 0; i < n; i++) q.put(i); } catch (InterruptedException e) { } });
        long t0 = System.nanoTime();
        p.start();
        long s = 0; for (int i = 0; i < n; i++) s += q.take();
        p.join();
        return (System.nanoTime() - t0) / 1000;
    }

    /** Transfers with tryLock(timeout): on timeout the transfer backs off, releases, and retries. */
    static final class TryAccount { final ReentrantLock lock = new ReentrantLock(); long balance = 1000; final int id; TryAccount(int id) { this.id = id; } }
    static int backoffs = 0;
    static synchronized void countBackoff() { backoffs++; }

    static boolean tryTransfer(TryAccount from, TryAccount to, long amt) throws InterruptedException {
        while (true) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            from.balance -= amt; to.balance += amt;
                            return true;
                        } finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }
            }
            countBackoff();
            Thread.sleep(1);
        }
    }

    /** Lock-ordered transfers, for comparison. */
    static final class OrderedAccount { final Object lock = new Object(); long balance = 1000; final int id; OrderedAccount(int id) { this.id = id; } }
    static void orderedTransfer(OrderedAccount from, OrderedAccount to, long amt) {
        OrderedAccount first = from.id < to.id ? from : to, second = from.id < to.id ? to : from;
        synchronized (first.lock) { synchronized (second.lock) { from.balance -= amt; to.balance += amt; } }
    }

    public static void main(String[] args) throws Exception {
        int n = 200_000;
        long[] w = new long[5], a = new long[5];
        for (int r = 0; r < 5; r++) { w[r] = wait_(n); a[r] = abq(n); }
        Arrays.sort(w); Arrays.sort(a);
        System.out.printf("== put/take of %d items, one producer, one consumer, capacity 5 (median of 5 runs) ==%n", n);
        System.out.printf("  wait/notifyAll counter: %d us total, %.1f ns per item%n", w[2], w[2] * 1000.0 / n);
        System.out.printf("  ArrayBlockingQueue:     %d us total, %.1f ns per item%n", a[2], a[2] * 1000.0 / n);

        int transfers = 2000;
        TryAccount[] acc = {new TryAccount(1), new TryAccount(2)};
        long t0 = System.nanoTime();
        Thread t1 = new Thread(() -> { try { for (int i = 0; i < transfers; i++) tryTransfer(acc[0], acc[1], 1); } catch (InterruptedException e) { } });
        Thread t2 = new Thread(() -> { try { for (int i = 0; i < transfers; i++) tryTransfer(acc[1], acc[0], 1); } catch (InterruptedException e) { } });
        t1.start(); t2.start(); t1.join(); t2.join();
        long tryUs = (System.nanoTime() - t0) / 1000;
        System.out.printf("== %d opposite transfers in each direction ==%n", transfers);
        System.out.printf("  tryLock with timeout: %d us total, back-offs = %d, balances total = %d%n", tryUs, backoffs, acc[0].balance + acc[1].balance);

        OrderedAccount[] oa = {new OrderedAccount(1), new OrderedAccount(2)};
        t0 = System.nanoTime();
        Thread o1 = new Thread(() -> { for (int i = 0; i < transfers; i++) orderedTransfer(oa[0], oa[1], 1); });
        Thread o2 = new Thread(() -> { for (int i = 0; i < transfers; i++) orderedTransfer(oa[1], oa[0], 1); });
        o1.start(); o2.start(); o1.join(); o2.join();
        long ordUs = (System.nanoTime() - t0) / 1000;
        System.out.printf("  lock ordering:        %d us total, balances total = %d%n", ordUs, oa[0].balance + oa[1].balance);
        System.exit(0);
    }
}
