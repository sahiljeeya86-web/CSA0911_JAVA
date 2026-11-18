/** A scanner station created by extending Thread. */
class StationThread extends Thread {
    private final int scripts;
    private final long delayMs;
    volatile int scanned = 0;
    StationThread(String name, int scripts, long delayMs) { super(name); this.scripts = scripts; this.delayMs = delayMs; }

    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            scanned++;
        }
        System.out.println(getName() + " finished " + scanned + " scripts");
    }
}

/** The same station created by implementing Runnable. */
class StationRunnable implements Runnable {
    private final String name;
    private final int scripts;
    private final long delayMs;
    int scanned = 0;
    StationRunnable(String name, int scripts, long delayMs) { this.name = name; this.scripts = scripts; this.delayMs = delayMs; }

    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            scanned++;
        }
        System.out.println(Thread.currentThread().getName() + " finished " + scanned + " scripts");
    }
}
