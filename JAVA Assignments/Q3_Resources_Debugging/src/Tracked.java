import java.io.*;
import java.util.*;

/** A reader that records every open and close. Replaces a debugger: the stack trace of each open
 *  shows where the file was opened, and the open list at the end shows what leaked. */
class Tracked extends BufferedReader {
    static final Map<String, Throwable> OPEN = new LinkedHashMap<>();
    static final List<String> EVENTS = new ArrayList<>();
    static int opened = 0, closed = 0, current = 0, peak = 0;
    final String name;
    private boolean closedFlag = false;

    Tracked(String name) throws IOException {
        super(new FileReader(name));
        this.name = name;
        opened++;
        current++;
        peak = Math.max(peak, current);
        OPEN.put(name, new Throwable("opened here"));
        EVENTS.add("open  " + name);
    }

    @Override public void close() throws IOException {
        if (!closedFlag) {
            closedFlag = true;
            closed++;
            current--;
            OPEN.remove(name);
            EVENTS.add("close " + name);
        }
        super.close();
    }

    static void resetCounters() { OPEN.clear(); EVENTS.clear(); opened = 0; closed = 0; current = 0; peak = 0; }
}
