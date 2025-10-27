package bankapp.security;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class AuthProvider_2 implements AuthProvider {
    private final int iterations;
    private final int keyLength;
    private final SecureRandom rng = new SecureRandom();

    public AuthProvider_2() {
        this(100_000, 256);
    }

    public AuthProvider_2(int iterations, int keyLengthBits) {
        this.iterations = iterations;
        this.keyLength = keyLengthBits;
    }

    @Override
    public byte[] generateSalt() {
        byte[] s = new byte[16];
        rng.nextBytes(s);
        return s;
    }

    @Override
    public String hashPin(String pin, byte[] salt) {
        if (pin == null)
            pin = "";
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] dk = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(dk);
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 failure", e);
        }
    }

    @Override
    public boolean verifyPin(String pin, String storedBase64Hash, byte[] salt) {
        String computed = hashPin(pin, salt);
        return constantTimeEquals(storedBase64Hash, computed);
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] x = a == null ? new byte[0] : a.getBytes();
        byte[] y = b == null ? new byte[0] : b.getBytes();
        if (x.length != y.length)
            return false;
        int r = 0;
        for (int i = 0; i < x.length; i++)
            r |= x[i] ^ y[i];
        return r == 0;
    }
}
