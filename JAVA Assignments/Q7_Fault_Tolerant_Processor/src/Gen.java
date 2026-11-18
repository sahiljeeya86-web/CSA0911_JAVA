import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Generates score files. Categories: valid, corrupt (non-numeric score), missing (not written), poison (a line with no score). */
public class Gen {
    static final String VALID = "valid", CORRUPT = "corrupt", MISSING = "missing", POISON = "poison";

    static String content(int i) {
        StringBuilder sb = new StringBuilder("student,S" + String.format("%05d", i) + "\n");
        for (int s = 1; s <= 5; s++) sb.append("mark,SUB").append(s).append(',').append((i * 7 + s * 13) % 101).append('\n');
        return sb.toString();
    }

    /** Writes n files named s00000.txt..; returns the category of each file index. */
    public static String[] generate(Path dir, int n, long seed) throws IOException {
        Files.createDirectories(dir);
        Random r = new Random(seed);
        String[] cat = new String[n];
        for (int i = 0; i < n; i++) {
            Path p = dir.resolve(String.format("s%05d.txt", i));
            double x = r.nextDouble();
            if (x < 0.90) { cat[i] = VALID; Files.writeString(p, content(i)); }
            else if (x < 0.95) { cat[i] = CORRUPT; Files.writeString(p, content(i).replace("mark,SUB3,", "mark,SUB3,x")); }
            else if (x < 0.96) { cat[i] = MISSING; }                      // file deliberately not written
            else { cat[i] = POISON; Files.writeString(p, "student,S00000\nmark,SUB1\n"); }
        }
        return cat;
    }

    /** Hand-checked set: 10 files, 7 valid, 2 corrupt (h03, h07), 1 missing (h05). Expected summary: ok 7, corrupt 2, missing 1. */
    public static void handSet(Path dir) throws IOException {
        Files.createDirectories(dir);
        for (int i = 0; i < 10; i++) {
            if (i == 5) continue;
            String text = content(i);
            if (i == 3 || i == 7) text = text.replace("mark,SUB3,", "mark,SUB3,x");
            Files.writeString(dir.resolve(String.format("h%02d.txt", i)), text);
        }
    }
}
