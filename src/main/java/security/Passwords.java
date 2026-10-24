package security;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
    public static final String SCHEME = "PBKDF2-210000";
    private Passwords() { }
    public static String salt() {
        byte[] b = new byte[16]; new SecureRandom().nextBytes(b);
        return hex(b);
    }
    public static String hash(char[] password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt.getBytes(StandardCharsets.UTF_8), 210000, 256);
        try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    public static boolean matches(char[] password, String salt, String hash, String scheme) {
        String candidate;
        if (SCHEME.equals(scheme)) candidate = hash(password, salt);
        else if ("SHA256".equals(scheme)) {
            try { candidate = hex(MessageDigest.getInstance("SHA-256").digest((salt + new String(password)).getBytes(StandardCharsets.UTF_8))); }
            catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        } else return false;
        return MessageDigest.isEqual(candidate.getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8));
    }
    public static void validate(char[] password) {
        if (password.length < 8 || password.length > 128) throw new IllegalArgumentException("Use a password with 8 to 128 characters.");
    }
    private static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder(); for (byte b : bytes) s.append(String.format("%02x", b & 255)); return s.toString();
    }
}
