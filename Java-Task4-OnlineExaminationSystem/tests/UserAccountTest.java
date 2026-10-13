public class UserAccountTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("Check " + checks); }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { checks++; return; }
        throw new AssertionError("Expected validation failure");
    }
    public static void main(String[] args) {
        char[] original = "Student123!".toCharArray();
        UserAccount user = new UserAccount("student", "Student", original);
        original[0] = 'x';
        check(user.authenticate("student", "Student123!".toCharArray()));
        check(!user.authenticate("wrong", "Student123!".toCharArray()));
        check(!user.authenticate("student", "wrong".toCharArray()));
        user.updateProfile("  Sanele  ", new char[0]);
        check(user.displayName().equals("Sanele"));
        check(user.authenticate("student", "Student123!".toCharArray()));
        rejects(() -> user.updateProfile("", "NewPass123!".toCharArray()));
        rejects(() -> user.updateProfile("Changed", "short".toCharArray()));
        check(user.displayName().equals("Sanele"));
        check(user.authenticate("student", "Student123!".toCharArray()));
        user.updateProfile("Sanele", "NewPass123!".toCharArray());
        check(!user.authenticate("student", "Student123!".toCharArray()));
        check(user.authenticate("student", "NewPass123!".toCharArray()));
        rejects(() -> new UserAccount("student", "Student", new char[0]));
        check(!user.authenticate("student", null));
        System.out.println("User profile checks passed: " + checks);
    }
}
