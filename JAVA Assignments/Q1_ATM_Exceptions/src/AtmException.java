/** Checked exception for business failures the customer can be told about. */
class AtmException extends Exception {
    AtmException(String message) { super(message); }
}

/** Checked: the account exists but holds less money than requested. */
class InsufficientFundsException extends AtmException {
    InsufficientFundsException(long requested, long balance) {
        super("requested " + requested + " but balance is " + balance);
    }
}

/** Checked: no such account in the card-reader file. */
class UnknownAccountException extends AtmException {
    UnknownAccountException(long account) { super("unknown account " + account); }
}
