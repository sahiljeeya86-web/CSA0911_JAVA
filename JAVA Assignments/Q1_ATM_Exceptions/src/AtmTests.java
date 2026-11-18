import java.io.*;
import java.nio.file.*;

/** Test table: each input chosen to take a different path through withdraw(). */
public class AtmTests {
    static void row(String id, String account, String amount, Path card, String intent) {
        String outcome;
        String caught;
        boolean fin;
        try {
            Atm.Result r = Atm.withdraw(account, amount, card);
            outcome = r.outcome; caught = r.catchBlock; fin = r.finallyRan;
        } catch (RuntimeException e) {
            // an exception that no catch block handles: the finally block of withdraw() has already run
            outcome = "UNCAUGHT " + e.getClass().getName(); caught = "none (propagated)"; fin = true;
        }
        System.out.printf("%-4s %-30s acc=%-8s amt=%-8s -> %-62s | catch: %-30s | finally: %s%n",
                id, intent, account, amount, outcome, caught, fin ? "ran" : "did not run");
    }

    public static void main(String[] args) throws IOException {
        Path card = Paths.get("accounts.txt");
        Files.write(card, java.util.Arrays.asList("1001 1000", "1002 200", "1003 0"));
        System.out.println("T   intent                         inputs                                                -> outcome");
        row("T1", "1001", "500", card, "valid withdrawal");
        row("T2", "1001", "abc", card, "letters in the amount");
        row("T3", "10x1", "100", card, "letters in the account number");
        row("T4", "1001", "-200", card, "negative amount");
        row("T5", "1002", "5000", card, "more than the balance");
        row("T6", "1001", "100", Paths.get("missing_card.txt"), "card-reader file missing");
        row("T7", "9999", "100", card, "unknown account");
        row("T8", "1001", "0", card, "zero amount (boundary)");
        row("T9", null, "100", card, "null account (programming error)");
        row("T10", "1003", "1", card, "balance is zero (boundary)");
        System.out.println();
        System.out.println("audit lines written by the finally block: " + Atm.logLines);
        System.out.println();
        System.out.println("Stack trace of the uncaught exception in T9 (printed by the JVM when nothing catches it):");
        Atm.withdraw(null, "100", card);
    }
}
