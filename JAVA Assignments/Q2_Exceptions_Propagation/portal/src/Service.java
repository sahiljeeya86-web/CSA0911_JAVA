import java.util.*;

/** Service layer: applies the business rules; rethrows failures from storage unchanged. */
class Service {
    private final Storage storage = new Storage();
    private final Map<String, Set<String>> registrations = new HashMap<>();
    private final Map<String, Boolean> windowOpen = new HashMap<>();
    static final long LIMIT_KB = 2000;

    void register(String student, String course) { registrations.computeIfAbsent(course, k -> new HashSet<>()).add(student); }
    void setWindow(String exam, boolean open) { windowOpen.put(exam, open); }

    void submit(String student, String course, String exam, String file, long sizeKb)
            throws ExamException {
        if (student == null || course == null) throw new PortalConfigurationException("student and course are required");
        if (!Boolean.TRUE.equals(windowOpen.get(exam))) throw new WindowClosedException(exam);
        if (!registrations.getOrDefault(course, Set.of()).contains(student)) throw new NotRegisteredException(student, course);
        if (sizeKb > LIMIT_KB) throw new FileTooLargeException(file, sizeKb, LIMIT_KB);
        storage.save(file, sizeKb);
    }
}
