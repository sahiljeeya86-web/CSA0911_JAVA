import java.lang.management.*;

/** Part A: transfers between hostel wallets. Each transfer locks both accounts. Usage: java Transfers unsafe|ordered */
public class Transfers {
    static final class Account {
        final int id; long balance;
        Account(int id, long balance) { this.id = id; this.balance = balance; }
    }

    /** Unsafe: locks the source first, then the destination. Two opposite transfers can deadlock. */
    static void transferUnsafe(Account from, Account to, long amount) throws InterruptedException {
        synchronized (from) {
            Thread.sleep(50);                       // widens the window so the deadlock is reliable
            synchronized (to) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    /** Safe: always lock the account with the smaller id first (one global order for every transfer). */
    static void transferOrdered(Account from, Account to, long amount) throws InterruptedException {
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            Thread.sleep(50);
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    interface Transfer { void run(Account a, Account b, long amt) throws InterruptedException; }

    public static void main(String[] args) throws InterruptedException {
        boolean ordered = args.length > 0 && args[0].equals("ordered");
        Transfer t = ordered ? Transfers::transferOrdered : Transfers::transferUnsafe;
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread ab = new Thread(() -> { try { t.run(a, b, 100); } catch (InterruptedException e) { } }, "A-to-B");
        Thread ba = new Thread(() -> { try { t.run(b, a, 50); } catch (InterruptedException e) { } }, "B-to-A");
        ab.setDaemon(true); ba.setDaemon(true);
        ab.start(); ba.start();
        ab.join(1000); ba.join(1000);
        boolean stuck = ab.isAlive() || ba.isAlive();
        long[] dead = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        System.out.println((ordered ? "ordered lock order" : "unsafe lock order") + ": threads still alive after 1 s = " + stuck
                + "; JVM deadlock detector: " + (dead == null ? 0 : dead.length) + " deadlocked threads");
        if (dead != null) {
            for (ThreadInfo info : ManagementFactory.getThreadMXBean().getThreadInfo(dead)) {
                System.out.println("  " + info.getThreadName() + " is " + info.getThreadState() + " waiting for lock held by " + info.getLockOwnerName());
            }
        }
        System.out.println("  balances: A=" + a.balance + " B=" + b.balance + " total=" + (a.balance + b.balance) + " (expected 2000)");
        System.exit(0);
    }
}
