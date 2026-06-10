import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.io.OutputStream;

public class MainCLI {
    private static HashMap<String, Account> vaultData;
    private static char[] masterPassword;
    private static final Console console = System.console();
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) {
        if (args.length > 0) {
            handleArgs(args);
            return;
        }
        interactiveMode();
    }

    // Fungsi baru pengganti Scanner supaya input nggak bocor/delay
    private static String readInput(String prompt) {
        try {
            if (console != null) {
                String res = console.readLine(prompt);
                return res == null ? "" : res;
            } else {
                System.out.print(prompt);
                System.out.flush(); // Memaksa teks langsung muncul di terminal
                String res = reader.readLine();
                return res == null ? "" : res;
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static char[] readPassword(String prompt) {
        if (console != null) {
            return console.readPassword(prompt);
        } else {
            try {
                System.out.print(prompt + " (Awas: Input tidak disembunyikan): ");
                System.out.flush();
                String res = reader.readLine();
                return res != null ? res.toCharArray() : new char[0];
            } catch (Exception e) {
                return new char[0];
            }
        }
    }

    private static void handleArgs(String[] args) {
        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "init": initVault(); break;
                case "reset": resetVault(); break;
                case "add":
                case "get":
                case "list":
                case "delete":
                    if (login()) executeCommand(command, args);
                    break;
                case "gen":
                    int length = 20;
                    if (args.length == 3 && (args[1].equals("-l") || args[1].equals("--length"))) {
                        length = Integer.parseInt(args[2]);
                    }
                    String pwd = generatePassword(length);
                    System.out.println("Generated Password: " + pwd);
                    copyToClipboard(pwd);
                    break;
                default:
                    System.out.println("Perintah tidak dikenali.");
            }
        } catch (Exception e) {
            System.err.println("Terjadi kesalahan sistem: " + e.getMessage());
        }
    }

    private static void executeCommand(String command, String[] args) throws Exception {
        switch (command) {
            case "add": addEntry(); break;
            case "get":
                if (args.length > 1) getEntry(args[1]);
                else System.out.println("Sertakan nama layanan.");
                break;
            case "list": listEntries(); break;
            case "delete":
                if (args.length > 1) deleteEntry(args[1]);
                else System.out.println("Sertakan nama layanan.");
                break;
        }
    }

    private static void interactiveMode() {
        printBanner();
        if (!Vault.vaultExists()) {
            System.out.println("Kesalahan: Vault belum ditemukan. Jalankan 'java MainCLI init' terlebih dahulu.");
            return;
        }
        if (!login()) return;

        while (true) {
            System.out.println("\nMenu Utama: [list, add, get, delete, generate, quit]");
            String choice = readInput("Pilih aksi: ").toLowerCase().trim();

            try {
                switch (choice) {
                    case "list": listEntries(); break;
                    case "add": addEntry(); break;
                    case "get":
                        String getSvc = readInput("Masukkan nama layanan: ");
                        getEntry(getSvc);
                        break;
                    case "delete":
                        String delSvc = readInput("Masukkan nama layanan: ");
                        deleteEntry(delSvc);
                        break;
                    case "generate":
                        String pwd = generatePassword(20);
                        System.out.println("Generated Password: " + pwd);
                        copyToClipboard(pwd);
                        break;
                    case "quit":
                        System.out.println("Sistem ditutup. Selamat tinggal.");
                        return;
                    default:
                        System.out.println("Pilihan tidak valid.");
                }
            } catch (Exception e) {
                System.err.println("Kesalahan eksekusi: " + e.getMessage());
            }
        }
    }

    private static boolean login() {
        masterPassword = readPassword("Masukkan master password: ");
        try {
            vaultData = Vault.loadVault(masterPassword);
            return vaultData != null;
        } catch (Exception e) {
            System.out.println("Kesalahan: Dekripsi gagal. Master password salah atau berkas korup.");
            return false;
        }
    }

    private static void initVault() throws Exception {
        if (Vault.vaultExists()) {
            System.out.println("Peringatan: Vault sudah ada. Inisialisasi dibatalkan.");
            return;
        }
        System.out.println("--- Inisialisasi MyPW ---");
        char[] pwd1 = readPassword("Masukkan master password yang kuat: ");
        char[] pwd2 = readPassword("Konfirmasi master password: ");

        if (!new String(pwd1).equals(new String(pwd2))) {
            System.out.println("Kesalahan: Password tidak cocok.");
            return;
        }
        if (pwd1.length == 0) {
            System.out.println("Kesalahan: Password tidak boleh kosong.");
            return;
        }

        HashMap<String, Account> emptyVault = new HashMap<>();
        Vault.saveVault(emptyVault, pwd1);
        System.out.println("Vault berhasil diinisialisasi.");
    }

    private static void resetVault() throws Exception {
        if (!Vault.vaultExists()) {
            System.out.println("Tidak ada Vault yang ditemukan.");
            return;
        }
        System.out.println("PERINGATAN: Mereset akan menghapus seluruh data secara permanen.");
        String confirm = readInput("Ketik 'RESET' untuk konfirmasi: ");

        if (confirm.equals("RESET")) {
            Vault.deleteVault();
            System.out.println("Vault lama telah dihapus.");
            initVault();
        } else {
            System.out.println("Konfirmasi gagal. Reset dibatalkan.");
        }
    }

    private static void addEntry() throws Exception {
        System.out.println("\n--- Tambah Entri Baru ---");
        String service = readInput("Nama layanan (ex: Google): ").toLowerCase().trim();

        if (vaultData.containsKey(service)) {
            String overwrite = readInput("Layanan sudah ada. Timpa data? (y/n): ");
            if (!overwrite.equalsIgnoreCase("y")) return;
        }

        String username = readInput("Masukkan username/email: ").trim();

        String pwd;
        String gen = readInput("Generate password otomatis? (y/n): ");
        if (gen.equalsIgnoreCase("y")) {
            pwd = generatePassword(20);
            System.out.println("Generated password: " + pwd);
        } else {
            pwd = new String(readPassword("Masukkan password: "));
        }

        vaultData.put(service, new Account(username, pwd));
        Vault.saveVault(vaultData, masterPassword);
        System.out.println("Entri berhasil ditambahkan.");
    }

    private static void getEntry(String service) {
        service = service.toLowerCase().trim();
        if (!vaultData.containsKey(service)) {
            System.out.println("Kesalahan: Entri tidak ditemukan.");
            return;
        }
        Account acc = vaultData.get(service);
        System.out.println("--------------------------------");
        System.out.println("Username : " + acc.getUsername());
        System.out.println("Password : ********");
        System.out.println("--------------------------------");

        String copy = readInput("Salin password ke clipboard? (y/n): ");
        if (copy.equalsIgnoreCase("y")) {
            copyToClipboard(acc.getPassword());
        }
    }

    private static void listEntries() {
        if (vaultData == null || vaultData.isEmpty()) {
            System.out.println("Vault Anda kosong.");
            return;
        }
        System.out.println(String.format("%-20s | %s", "Layanan", "Username"));
        System.out.println("-----------------------------------------");
        for (Map.Entry<String, Account> entry : vaultData.entrySet()) {
            System.out.println(String.format("%-20s | %s", entry.getKey(), entry.getValue().getUsername()));
        }
    }

    private static void deleteEntry(String service) throws Exception {
        service = service.toLowerCase().trim();
        if (!vaultData.containsKey(service)) {
            System.out.println("Entri tidak ditemukan.");
            return;
        }
        String confirm = readInput("Anda yakin ingin menghapus '" + service + "'? (y/n): ");
        if (confirm.equalsIgnoreCase("y")) {
            vaultData.remove(service);
            Vault.saveVault(vaultData, masterPassword);
            System.out.println("Entri dihapus.");
        }
    }

    private static String generatePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static void copyToClipboard(String text) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            // Pendekatan 1: Pakai tool bawaan OS (Paling ampuh buat Linux CLI)
            if (os.contains("linux")) {
                if (runNativeClipboard(new String[]{"wl-copy"}, text)) return; // Untuk Wayland
                if (runNativeClipboard(new String[]{"xclip", "-selection", "clipboard"}, text)) return; // Untuk X11
                if (runNativeClipboard(new String[]{"xsel", "-b", "-i"}, text)) return; // Alternatif X11
            } else if (os.contains("win")) {
                if (runNativeClipboard(new String[]{"clip"}, text)) return;
            } else if (os.contains("mac")) {
                if (runNativeClipboard(new String[]{"pbcopy"}, text)) return;
            }

            // Pendekatan 2: Fallback ke Java AWT jika tool OS di atas tidak ada
            StringSelection stringSelection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            System.out.println("Disalin ke clipboard (via AWT).");
            
            // Hack kecil: kasih waktu AWT buat sinkronisasi sebelum program/terminal close
            Thread.sleep(500); 

        } catch (Exception e) {
            System.out.println("Gagal menyalin ke clipboard. Pastikan xclip atau wl-copy sudah terinstall di sistemmu.");
        }
    }

    // Fungsi baru untuk eksekusi command bawaan OS
    private static boolean runNativeClipboard(String[] command, String text) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            java.io.OutputStream os = process.getOutputStream();
            os.write(text.getBytes());
            os.close();
            process.waitFor();
            if (process.exitValue() == 0) {
                System.out.println("Disalin ke clipboard.");
                return true;
            }
        } catch (Exception e) {
            // Kalau toolnya nggak ada, abaikan error dan biarkan program coba tool berikutnya
        }
        return false;
    }

    private static void printBanner() {
        System.out.println("============================================");
        System.out.println("                 M Y P W                    ");
        System.out.println("            Java CLI Edition                ");
        System.out.println("============================================"); 
    }
}