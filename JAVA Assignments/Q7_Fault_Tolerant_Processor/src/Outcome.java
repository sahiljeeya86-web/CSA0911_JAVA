/** The result of one file: a status, and the total when the file was valid. */
final class Outcome {
    enum Status { OK, CORRUPT, MISSING, INTERNAL_ERROR, IO_ERROR }
    final String file; final Status status; final String student; final int total; final String detail;
    Outcome(String file, Status status, String student, int total, String detail) {
        this.file = file; this.status = status; this.student = student; this.total = total; this.detail = detail;
    }
}
