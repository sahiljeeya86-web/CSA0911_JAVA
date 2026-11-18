import java.io.IOException;

/** Storage layer: knows about files. Converts IOException into a meaningful exception with the cause kept. */
class Storage {
    static final long QUOTA_KB = 500;

    void save(String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > QUOTA_KB) throw new IOException("Disk quota exceeded");
            System.out.println("  storage: saved " + fileName);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, e);
        }
    }
}
