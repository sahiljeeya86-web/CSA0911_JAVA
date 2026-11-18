public class PortalDemo {
    static Controller build() {
        Service s = new Service();
        s.register("S101", "CSA0911");
        s.setWindow("CO3-Final", true);
        s.setWindow("CO3-Closed", false);
        return new Controller(s);
    }

    public static void main(String[] args) {
        Controller c = build();
        System.out.println("T1 window closed:  " + c.handleSubmit("S101", "CSA0911", "CO3-Closed", "a1.pdf", 100));
        System.out.println("T2 not registered: " + c.handleSubmit("S999", "CSA0911", "CO3-Final", "a2.pdf", 100));
        System.out.println("T3 file too large: " + c.handleSubmit("S101", "CSA0911", "CO3-Final", "a3.pdf", 2500));
        System.out.println("T4 storage fails:  " + c.handleSubmit("S101", "CSA0911", "CO3-Final", "a4.pdf", 800));
        System.out.println("T5 valid:          " + c.handleSubmit("S101", "CSA0911", "CO3-Final", "a5.pdf", 120));
        // chained stack trace for T4, written to the log by the caller
        try {
            new Service().submit("S101", "CSA0911", "CO3-Final", "x.pdf", 0);
        } catch (ExamException ignored) { }
        Service s = new Service();
        s.register("S101", "CSA0911");
        s.setWindow("CO3-Final", true);
        try {
            s.submit("S101", "CSA0911", "CO3-Final", "big.pdf", 800);
        } catch (ExamException e) {
            System.out.println("Chained trace for T4:");
            e.printStackTrace(System.out);
            System.out.println("getCause(): " + e.getCause());
        }
    }
}
