import ui.LaundryFrame;
import repository.JdbcTransaksiRepository;

import javax.swing.SwingUtilities;

// Titik masuk aplikasi: membuka GUI Swing dengan penyimpanan PostgreSQL.
public class Main {
    // Menjadwalkan pembuatan jendela pada thread Swing dan menampilkannya.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LaundryFrame(new JdbcTransaksiRepository()).setVisible(true));
    }
}
