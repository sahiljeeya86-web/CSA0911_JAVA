/** Base of every business failure in the portal. Checked: the caller must decide what to tell the student. */
class ExamException extends Exception {
    ExamException(String message) { super(message); }
    ExamException(String message, Throwable cause) { super(message, cause); }
}

/** The exam window has closed. */
class WindowClosedException extends ExamException {
    WindowClosedException(String exam) { super("The submission window for " + exam + " has closed"); }
}

/** The student is not registered for the course. */
class NotRegisteredException extends ExamException {
    NotRegisteredException(String student, String course) {
        super(student + " is not registered for " + course);
    }
}

/** The answer file is larger than the limit. */
class FileTooLargeException extends ExamException {
    FileTooLargeException(String file, long sizeKb, long limitKb) {
        super(file + " is " + sizeKb + " KB; the limit is " + limitKb + " KB");
    }
}

/** Storage failed; the original IOException is kept as the cause. */
class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String message, Throwable cause) { super(message, cause); }
}

/** Unchecked: a programming error (for example, a null argument), not a failure the student can cause. */
class PortalConfigurationException extends RuntimeException {
    PortalConfigurationException(String message) { super(message); }
}
