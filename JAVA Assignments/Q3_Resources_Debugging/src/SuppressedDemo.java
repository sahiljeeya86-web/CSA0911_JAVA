import java.io.*;

public class SuppressedDemo {
    static class Flaky implements AutoCloseable {
        final String name;
        Flaky(String name) { this.name = name; }
        @Override public void close() throws IOException {
            System.out.println("  close " + name);
            throw new IOException("close failed for " + name);
        }
    }
    public static void main(String[] args) {
        try (Flaky a = new Flaky("A"); Flaky b = new Flaky("B")) {
            System.out.println("  body runs and fails");
            throw new IllegalStateException("body failed");
        } catch (Exception e) {
            System.out.println("primary: " + e);
            for (Throwable s : e.getSuppressed()) System.out.println("  suppressed: " + s);
        }
    }
}
