import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** One file as a Callable. It never throws: every failure is converted into an Outcome, so one bad file cannot stop the run. */
final class ScriptTask implements Callable<Outcome> {
    final Path file; final long ioLatencyMs;
    ScriptTask(Path file, long ioLatencyMs) { this.file = file; this.ioLatencyMs = ioLatencyMs; }

    @Override public Outcome call() {
        String name = file.getFileName().toString();
        try {
            if (ioLatencyMs > 0) Thread.sleep(ioLatencyMs);             // stands in for disk or network latency
            List<String> lines = Files.readAllLines(file);
            String student = null;
            int total = 0;
            for (String line : lines) {
                String[] p = line.split(",");
                if (p[0].equals("student")) student = p[1];
                else if (p[0].equals("mark")) total += Integer.parseInt(p[2].trim());   // NumberFormatException = corrupt
            }
            return new Outcome(name, Outcome.Status.OK, student, total, "");
        } catch (NoSuchFileException e) {
            return new Outcome(name, Outcome.Status.MISSING, null, 0, "no such file");
        } catch (NumberFormatException e) {
            return new Outcome(name, Outcome.Status.CORRUPT, null, 0, "non-numeric score: " + e.getMessage());
        } catch (IOException e) {
            return new Outcome(name, Outcome.Status.IO_ERROR, null, 0, e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Outcome(name, Outcome.Status.IO_ERROR, null, 0, "interrupted");
        } catch (RuntimeException e) {                                   // unexpected: a poison file, or a bug
            return new Outcome(name, Outcome.Status.INTERNAL_ERROR, null, 0, e.toString());
        }
    }
}
