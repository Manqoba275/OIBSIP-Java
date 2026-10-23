import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;

public final class Passwords {
    private Passwords() {}
    public static byte[] salt() {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt); return salt;
    }
    public static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 210_000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception ex) { throw new IllegalStateException("Password hashing is unavailable.", ex); }
        finally { spec.clearPassword(); }
    }
}
