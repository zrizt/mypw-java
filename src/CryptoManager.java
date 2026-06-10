import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CryptoManager {
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BIT = 128;
    private static final int ITERATIONS = 480000;
    private static final int KEY_LENGTH = 256;
    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";

    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static byte[] encrypt(byte[] pText, char[] password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey secretKey = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] encryptedText = cipher.doFinal(pText);

        return ByteBuffer.allocate(salt.length + iv.length + encryptedText.length)
                .put(salt).put(iv).put(encryptedText).array();
    }

    public static byte[] decrypt(byte[] cText, char[] password) throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(cText);
        byte[] salt = new byte[SALT_LENGTH];
        bb.get(salt);
        byte[] iv = new byte[IV_LENGTH];
        bb.get(iv);
        byte[] encryptedText = new byte[bb.remaining()];
        bb.get(encryptedText);

        SecretKey secretKey = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        
        return cipher.doFinal(encryptedText);
    }
}