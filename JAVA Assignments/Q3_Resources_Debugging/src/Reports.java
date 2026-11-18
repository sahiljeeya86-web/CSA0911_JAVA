import java.io.*;
import java.util.*;

/** Three versions of the same report generator. The input files are 'store00.txt' ... 'store09.txt'. */
class Reports {
    /** Parses one line "item,qty,price" and adds qty*price to the total. Fails on a bad line. */
    static long parseInto(BufferedReader in, String file) throws IOException {
        long total = 0;
        String line;
        int lineNo = 0;
        while ((line = in.readLine()) != null) {
            lineNo++;
            String[] p = line.split(",");
            try {
                total += Long.parseLong(p[1].trim()) * Long.parseLong(p[2].trim());
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IOException("bad line " + lineNo + " in " + file + ": " + line, e);
            }
        }
        return total;
    }

    /** Version 1: manual close INSIDE the try body. If anything throws, the close() calls are skipped. */
    static long manualLeaky(String[] files) throws IOException {
        List<BufferedReader> open = new ArrayList<>();
        long total = 0;
        try {
            for (String f : files) {
                BufferedReader in = new Tracked(f);
                open.add(in);
                total += parseInto(in, f);
            }
            for (BufferedReader in : open) in.close();          // reached only if nothing failed
        } catch (IOException e) {
            System.out.println("  report failed: " + e.getMessage());
            throw e;
        }
        return total;
    }

    /** Version 2: manual close in a finally block, with a null check. Correct, but verbose. */
    static long manualFinally(String[] files) throws IOException {
        long total = 0;
        for (String f : files) {
            BufferedReader in = null;
            try {
                in = new Tracked(f);
                total += parseInto(in, f);
            } catch (IOException e) {
                System.out.println("  report failed on " + f + ": " + e.getMessage());
                throw e;
            } finally {
                if (in != null) in.close();
            }
        }
        return total;
    }

    /** Version 3: try-with-resources. Each reader is closed when its block ends, in reverse order. */
    static long tryWithResources(String[] files) throws IOException {
        long total = 0;
        for (String f : files) {
            try (BufferedReader in = new Tracked(f)) {
                total += parseInto(in, f);
            } catch (IOException e) {
                System.out.println("  report failed on " + f + ": " + e.getMessage());
                throw e;
            }
        }
        return total;
    }
}
