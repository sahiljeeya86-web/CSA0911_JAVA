/** Controller layer: the only place that decides what the student sees. */
class Controller {
    private final Service service;
    Controller(Service service) { this.service = service; }

    String handleSubmit(String student, String course, String exam, String file, long sizeKb) {
        try {
            service.submit(student, course, exam, file, sizeKb);
            return "Your answer " + file + " was accepted.";
        } catch (WindowClosedException | NotRegisteredException e) {
            return "Submission refused: " + e.getMessage() + ".";
        } catch (FileTooLargeException e) {
            return "Submission refused: " + e.getMessage() + ". Please compress the file.";
        } catch (SubmissionStorageException e) {
            // the student sees a short message; the full chain is written to the log by the caller
            return "Your answer could not be saved. Please try again later. (reference " + e.getMessage() + ")";
        } catch (ExamException e) {
            return "Submission refused: " + e.getMessage();
        }
    }
}
