import java.io.*;
import java.util.HashMap;

public class Vault {
    private static final String VAULT_PATH = System.getProperty("user.home") + File.separator + ".mypw_vault.enc";

    public static boolean vaultExists() {
        return new File(VAULT_PATH).exists();
    }

    public static void saveVault(HashMap<String, Account> accounts, char[] masterPassword) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(accounts);
        oos.flush();
        
        byte[] serializedData = baos.toByteArray();
        byte[] encryptedData = CryptoManager.encrypt(serializedData, masterPassword);

        try (FileOutputStream fos = new FileOutputStream(VAULT_PATH)) {
            fos.write(encryptedData);
        }
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Account> loadVault(char[] masterPassword) throws Exception {
        File file = new File(VAULT_PATH);
        if (!file.exists()) return null;

        byte[] encryptedData = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(encryptedData);
        }

        byte[] decryptedData = CryptoManager.decrypt(encryptedData, masterPassword);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(decryptedData);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (HashMap<String, Account>) ois.readObject();
    }

    public static void deleteVault() {
        File file = new File(VAULT_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}