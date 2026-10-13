import java.util.Arrays;

public final class UserAccount {
    private final String username;
    private String displayName;
    private char[] password;

    public UserAccount(String username, String displayName, char[] password) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required.");
        this.username = username;
        updateProfile(displayName, password);
    }
    public String displayName() { return displayName; }
    public boolean authenticate(String name, char[] candidate) {
        return username.equals(name) && Arrays.equals(password, candidate);
    }
    public void updateProfile(String name, char[] newPassword) {
        if (name == null || name.isBlank() || name.trim().length() > 60) {
            throw new IllegalArgumentException("Enter a display name of 1 to 60 characters.");
        }
        boolean replace = newPassword != null && newPassword.length > 0;
        if ((password == null || replace) && (newPassword == null || newPassword.length < 8 || newPassword.length > 128)) {
            throw new IllegalArgumentException("Password must contain 8 to 128 characters.");
        }
        // Validate every field before changing the profile; rejected edits leave it intact.
        if (replace) {
            if (password != null) Arrays.fill(password, '\0');
            password = newPassword.clone();
        }
        displayName = name.trim();
    }
}
