package bankapp.security;

public interface AuthProvider {
    byte[] generateSalt();

    String hashPin(String pin, byte[] salt);

    boolean verifyPin(String pin, String storedBase64Hash, byte[] salt);
}
