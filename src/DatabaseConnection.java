import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// Membaca konfigurasi lokal dan membuka koneksi JDBC ke PostgreSQL.
public class DatabaseConnection {
    // Membuka koneksi dari konfigurasi; pemanggil bertanggung jawab menutup koneksi.
    public static Connection buka() throws IOException, SQLException {
        return buka(Path.of("config/database.properties"));
    }

    // Membuka koneksi dari konfigurasi; pemanggil bertanggung jawab menutup koneksi.
    public static Connection buka(Path fileKonfigurasi) throws IOException, SQLException {
        Properties konfigurasi = new Properties();
        try (Reader reader = Files.newBufferedReader(fileKonfigurasi, StandardCharsets.UTF_8)) {
            konfigurasi.load(reader);
        }
        String url = konfigurasi.getProperty("db.url", "").trim();
        String user = konfigurasi.getProperty("db.user", "").trim();
        String password = konfigurasi.getProperty("db.password");
        if (!url.startsWith("jdbc:postgresql://") || user.isEmpty() || password == null
                || password.equals("ISI_PASSWORD_LOKAL")) {
            throw new IllegalArgumentException(
                    "Isi db.url, db.user, dan db.password pada konfigurasi lokal terlebih dahulu.");
        }
        Properties koneksi = new Properties();
        koneksi.setProperty("user", user);
        koneksi.setProperty("password", password);
        koneksi.setProperty("connectTimeout", "5");
        koneksi.setProperty("socketTimeout", "10");
        return DriverManager.getConnection(url, koneksi);
    }
}
