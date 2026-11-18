public class BroaderFirst {
    static long parse(String s) {
        try {
            return Long.parseLong(s);
        } catch (IllegalArgumentException e) {        // broader parent first
            return -1;
        } catch (NumberFormatException e) {           // narrower child second: unreachable
            return -2;
        }
    }
    public static void main(String[] args) { System.out.println(parse("12")); }
}
