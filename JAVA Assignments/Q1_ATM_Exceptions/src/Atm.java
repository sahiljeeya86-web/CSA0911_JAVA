import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Atm {
    /** Outcome of one withdrawal attempt, recorded for the test table. */
    static final class Result {
        final String outcome, catchBlock; final boolean finallyRan;
        Result(String outcome, String catchBlock, boolean finallyRan) {
            this.outcome = outcome; this.catchBlock = catchBlock; this.finallyRan = finallyRan;
        }
    }

    static int logLines = 0;   // the finally block writes one audit line per attempt

    /** Reads "account balance" pairs from the card-reader file. Throws IOException if it is missing. */
    static Map<Long, Long> loadBalances(Path cardFile) throws IOException {
        Map<Long, Long> balances = new HashMap<>();
        try (BufferedReader in = Files.newBufferedReader(cardFile)) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                balances.put(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
            }
        }
        return balances;
    }

    /** One withdrawal. Each failure point has its own catch block; the finally block always runs. */
    static Result withdraw(String accountText, String amountText, Path cardFile) {
        String outcome;
        String catchBlock = "none";
        try {
            long account = Long.parseLong(accountText.trim());            // NumberFormatException
            long amount = Long.parseLong(amountText.trim());              // NumberFormatException
            if (amount <= 0) throw new IllegalArgumentException("amount must be positive, got " + amount);
            Map<Long, Long> balances = loadBalances(cardFile);            // IOException (checked)
            Long balance = balances.get(account);
            if (balance == null) throw new UnknownAccountException(account);
            if (amount > balance) throw new InsufficientFundsException(amount, balance);
            outcome = "DISPENSED " + amount + ", new balance " + (balance - amount);
            catchBlock = "no exception";
        } catch (NumberFormatException e) {                               // narrower: must come first
            outcome = "REJECTED - not a number: " + e.getMessage();
            catchBlock = "NumberFormatException";
        } catch (IllegalArgumentException e) {                            // broader parent of the one above
            outcome = "REJECTED - " + e.getMessage();
            catchBlock = "IllegalArgumentException";
        } catch (InsufficientFundsException e) {
            outcome = "REJECTED - " + e.getMessage();
            catchBlock = "InsufficientFundsException";
        } catch (UnknownAccountException e) {
            outcome = "REJECTED - " + e.getMessage();
            catchBlock = "UnknownAccountException";
        } catch (AtmException e) {                                        // any other business failure
            outcome = "REJECTED - " + e.getMessage();
            catchBlock = "AtmException";
        } catch (IOException e) {                                         // card reader missing or unreadable
            outcome = "CARD READER ERROR - " + e.getClass().getSimpleName() + ": " + e.getMessage();
            catchBlock = "IOException";
        } finally {
            logLines++;                                                  // runs on every path, including uncaught ones
        }
        return new Result(outcome, catchBlock, true);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("usage: java Atm <account> <amount> <cardfile>");
            return;
        }
        Result r = withdraw(args[0], args[1], Paths.get(args[2]));
        System.out.println(r.outcome);
    }
}
