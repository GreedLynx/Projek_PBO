package ui;

import service.LaundryService;
import model.TransaksiLaundry;
import model.Layanan;
import repository.TransaksiRepository;
import repository.JdbcTransaksiRepository;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// Mengatur form, tabel, event pengguna, dan operasi repository melalui SwingWorker.
public class LaundryFrame extends JFrame {
    private final TransaksiRepository repository;
    private final JButton resetButton = new JButton("Reset");
    private boolean sibuk;
    private List<TransaksiLaundry> daftarTransaksi = List.of();
    private TransaksiLaundry transaksiTerpilih;
    private final JButton tambahButton = new JButton("Tambah");
    private final JButton ubahButton = new JButton("Ubah");
    private final JButton hapusButton = new JButton("Hapus");
    private final JLabel jumlahLabel = new JLabel("Belum ada transaksi.");
    private final JLabel identitasLabel = new JLabel("Transaksi baru");
    private final JTextField namaField = new JTextField(18);
    private final JTextField beratField = new JTextField(18);
    private final JComboBox<Layanan> layananBox = new JComboBox<>(Layanan.values());
    private final NumberFormat formatAngka = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"));
    private final JLabel pesanLabel = new JLabel("Isi data pelanggan dan layanan untuk menyiapkan transaksi.");
    private final JTextField hargaField = new JTextField("-", 18);
    private final JTextField totalField = new JTextField("-", 18);
    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[] { "Diproses", "Selesai", "Diambil" });
    private final JTable tabel = new JTable(new DefaultTableModel(
            new String[] { "ID", "Nama pelanggan", "Berat (kg)", "Layanan",
                    "Harga/kg (Rp)", "Total (Rp)", "Status", "Tanggal masuk" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    });

    // Menerima repository, menyusun GUI, memasang event, lalu memuat daftar awal.
    public LaundryFrame(TransaksiRepository repository) {
        super("LaundryKos - Pengelolaan Transaksi");
        this.repository = Objects.requireNonNull(repository);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel konten = new JPanel(new BorderLayout(16, 16));
        konten.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel judulPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        JLabel judul = new JLabel("LaundryKos");
        judul.setFont(judul.getFont().deriveFont(Font.BOLD, 26f));
        judul.setForeground(new Color(15, 90, 90));
        judulPanel.add(judul);
        judulPanel.add(new JLabel("Pengelolaan transaksi laundry anak kos"));
        konten.add(judulPanel, BorderLayout.NORTH);

        JSplitPane pembagi = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buatForm(), buatPanelTabel());
        pembagi.setResizeWeight(0);
        pembagi.setDividerLocation(370);
        pembagi.setBorder(null);
        konten.add(pembagi, BorderLayout.CENTER);
        konten.add(pesanLabel, BorderLayout.SOUTH);
        setContentPane(konten);
        setMinimumSize(new Dimension(900, 560));
        setSize(1150, 650);
        setLocationRelativeTo(null);
        beratField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { perbaruiBiaya(); }
            public void removeUpdate(DocumentEvent event) { perbaruiBiaya(); }
            public void changedUpdate(DocumentEvent event) { perbaruiBiaya(); }
        });
        layananBox.addActionListener(event -> perbaruiBiaya());
        FocusAdapter validasi = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                try {
                    if (event.getComponent() == namaField) {
                        LaundryService.validasiNama(namaField.getText());
                        pesanLabel.setText("Nama pelanggan valid.");
                    } else {
                        LaundryService.bacaBerat(beratField.getText());
                        pesanLabel.setText("Berat valid. Total dihitung otomatis.");
                    }
                } catch (IllegalArgumentException error) {
                    pesanLabel.setText(error.getMessage());
                }
            }
        };
        namaField.addFocusListener(validasi);
        beratField.addFocusListener(validasi);
        tabel.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                pilihTransaksi();
            }
        });
        tambahButton.addActionListener(event -> simpanTransaksi());
        ubahButton.addActionListener(event -> simpanTransaksi());
        hapusButton.addActionListener(event -> hapusTransaksi());
        resetForm();
        muatUlang("");
    }

    // Menyusun isian dan tombol; harga serta total hanya dapat dibaca pengguna.
    private JPanel buatForm() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createTitledBorder("Form transaksi"));
        panel.add(identitasLabel, BorderLayout.NORTH);
        panel.setMinimumSize(new Dimension(340, 0));
        JPanel isian = new JPanel(new GridBagLayout());
        hargaField.setEditable(false);
        totalField.setEditable(false);
        statusBox.setEnabled(false);
        beratField.setToolTipText("Berat dalam kg, contoh: 2,5 atau 2.5");
        tambahIsian(isian, 0, "Nama pelanggan", namaField);
        tambahIsian(isian, 1, "Berat (kg)", beratField);
        tambahIsian(isian, 2, "Layanan", layananBox);
        tambahIsian(isian, 3, "Harga/kg (Rp)", hargaField);
        tambahIsian(isian, 4, "Total (Rp)", totalField);
        tambahIsian(isian, 5, "Status", statusBox);
        GridBagConstraints ruang = new GridBagConstraints();
        ruang.gridy = 6;
        ruang.weighty = 1;
        isian.add(new JPanel(), ruang);
        panel.add(new JScrollPane(isian), BorderLayout.CENTER);

        JPanel tombolPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        tombolPanel.add(tambahButton);
        tombolPanel.add(ubahButton);
        tombolPanel.add(hapusButton);
        resetButton.addActionListener(event -> {
            resetForm();
            muatUlang("");
        });
        tombolPanel.add(resetButton);
        panel.add(tombolPanel, BorderLayout.SOUTH);
        return panel;
    }

    // Menempatkan label dan input pada satu baris GridBagLayout.
    private void tambahIsian(JPanel panel, int baris, String teks, JComponent input) {
        GridBagConstraints posisi = new GridBagConstraints();
        posisi.gridx = 0;
        posisi.gridy = baris;
        posisi.insets = new Insets(8, 8, 8, 8);
        posisi.anchor = GridBagConstraints.LINE_START;
        JLabel label = new JLabel(teks);
        label.setLabelFor(input);
        panel.add(label, posisi);
        posisi.gridx = 1;
        posisi.weightx = 1;
        posisi.fill = GridBagConstraints.HORIZONTAL;
        panel.add(input, posisi);
    }

    // Menyiapkan tabel pilihan tunggal dengan kolom dan area gulir.
    private JPanel buatPanelTabel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Daftar transaksi"));
        panel.setMinimumSize(new Dimension(400, 0));
        tabel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabel.setRowHeight(28);
        tabel.setFillsViewportHeight(true);
        tabel.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabel.getTableHeader().setReorderingAllowed(false);
        tabel.getAccessibleContext().setAccessibleName("Daftar transaksi laundry");
        int[] lebar = { 60, 170, 90, 130, 120, 120, 100, 160 };
        for (int i = 0; i < lebar.length; i++) {
            tabel.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
        panel.add(new JScrollPane(tabel), BorderLayout.CENTER);
        panel.add(jumlahLabel, BorderLayout.SOUTH);
        return panel;
    }

    // Menghapus pilihan dan isian lalu mengembalikan tombol ke mode tambah.
    private void resetForm() {
        transaksiTerpilih = null;
        tabel.clearSelection();
        tambahButton.setEnabled(true);
        ubahButton.setEnabled(false);
        hapusButton.setEnabled(false);
        statusBox.setEnabled(false);
        identitasLabel.setText("Transaksi baru");
        namaField.setText("");
        beratField.setText("");
        layananBox.setSelectedIndex(0);
        perbaruiBiaya();
        statusBox.setSelectedIndex(0);
        namaField.requestFocusInWindow();
        pesanLabel.setText("Isi data pelanggan dan layanan untuk menyiapkan transaksi.");
    }

    // Memperbarui pratinjau harga dan total; input berat belum valid ditampilkan sebagai tanda minus.
    private void perbaruiBiaya() {
        Layanan layanan = (Layanan) layananBox.getSelectedItem();
        BigDecimal harga = transaksiTerpilih != null && layanan == transaksiTerpilih.getLayanan()
                ? transaksiTerpilih.getHargaPerKg() : layanan.getHargaPerKg();
        hargaField.setText(formatAngka.format(harga));
        try {
            totalField.setText(formatAngka.format(LaundryService.hitungTotal(
                    LaundryService.bacaBerat(beratField.getText()), harga)));
        } catch (IllegalArgumentException error) {
            totalField.setText("-");
        }
    }

    // Mengisi ulang model tabel dan menyimpan daftar objek yang sesuai dengan barisnya.
    private void tampilkanTabel(List<TransaksiLaundry> terbaru) {
        tabel.setEnabled(true);
        daftarTransaksi = terbaru;
        DefaultTableModel model = (DefaultTableModel) tabel.getModel();
        model.setRowCount(0);
        for (TransaksiLaundry transaksi : terbaru) {
            model.addRow(new Object[] { transaksi.getId(), transaksi.getNamaPelanggan(),
                    formatAngka.format(transaksi.getBeratKg()), transaksi.getLayanan(),
                    formatAngka.format(transaksi.getHargaPerKg()), formatAngka.format(transaksi.getTotalBiaya()),
                    transaksi.getStatus(), transaksi.getTanggalMasuk().format(
                            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) });
        }
        jumlahLabel.setText((terbaru.isEmpty() ? "Belum ada transaksi." : terbaru.size() + " transaksi.")
                + (repository instanceof JdbcTransaksiRepository ? " Data tersimpan di PostgreSQL."
                        : " Data sementara selama aplikasi berjalan."));
    }

    // Mengonversi indeks tampilan ke model lalu mengisi form dari transaksi terpilih.
    private void pilihTransaksi() {
        int baris = tabel.getSelectedRow();
        if (baris < 0) {
            if (transaksiTerpilih != null) {
                resetForm();
            }
            return;
        }
        transaksiTerpilih = daftarTransaksi.get(tabel.convertRowIndexToModel(baris));
        namaField.setText(transaksiTerpilih.getNamaPelanggan());
        beratField.setText(transaksiTerpilih.getBeratKg().toPlainString());
        layananBox.setSelectedItem(transaksiTerpilih.getLayanan());
        statusBox.setSelectedItem(transaksiTerpilih.getStatus());
        statusBox.setEnabled(true);
        tambahButton.setEnabled(false);
        ubahButton.setEnabled(true);
        hapusButton.setEnabled(true);
        identitasLabel.setText("Edit transaksi #" + transaksiTerpilih.getId());
        perbaruiBiaya();
        pesanLabel.setText("Ubah data atau tekan Reset untuk menambah transaksi baru.");
    }

    // Membuat transaksi tervalidasi lalu memilih operasi tambah atau ubah pada repository.
    private void simpanTransaksi() {
        if (sibuk) return;
        try {
            BigDecimal berat = LaundryService.bacaBerat(beratField.getText());
            Layanan layanan = (Layanan) layananBox.getSelectedItem();
            boolean baru = transaksiTerpilih == null;
            TransaksiLaundry transaksi = baru
                    ? new TransaksiLaundry(namaField.getText(), berat, layanan)
                    : transaksiTerpilih.ubah(namaField.getText(), berat, layanan,
                            (String) statusBox.getSelectedItem());
            kerjakan(() -> baru ? repository.tambah(transaksi) : repository.ubah(transaksi),
                    hasil -> selesaiOperasi("Transaksi berhasil disimpan."),
                    error -> pesanLabel.setText(error.getMessage()));
        } catch (IllegalArgumentException error) {
            pesanLabel.setText(error.getMessage());
        }
    }

    // Meminta konfirmasi sebelum menjalankan penghapusan transaksi terpilih.
    private void hapusTransaksi() {
        if (sibuk) return;
        if (transaksiTerpilih == null) {
            pesanLabel.setText("Pilih transaksi yang akan dihapus.");
            return;
        }
        int jawaban = JOptionPane.showConfirmDialog(this,
                "Hapus transaksi #" + transaksiTerpilih.getId() + " milik "
                        + transaksiTerpilih.getNamaPelanggan() + "?",
                "Konfirmasi hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (jawaban != JOptionPane.YES_OPTION) {
            return;
        }
        long id = transaksiTerpilih.getId();
        kerjakan(() -> { repository.hapus(id); return null; },
                hasil -> selesaiOperasi("Transaksi berhasil dihapus."),
                error -> pesanLabel.setText(error.getMessage()));
    }

    // Mengembalikan form ke mode tambah dan memuat data setelah operasi berhasil.
    private void selesaiOperasi(String pesan) {
        resetForm();
        muatUlang(pesan);
    }

    // Mengambil daftar terbaru; kegagalan mematikan aksi data sampai pengguna mencoba Reset.
    private void muatUlang(String pesan) {
        kerjakan(repository::daftar, terbaru -> {
            tampilkanTabel(terbaru);
            if (!pesan.isEmpty()) pesanLabel.setText(pesan);
        }, error -> {
            pesanLabel.setText(pesan + " Daftar gagal dimuat. " + error.getMessage()
                    + " Tekan Reset untuk memuat ulang.");
            tambahButton.setEnabled(false);
            ubahButton.setEnabled(false);
            hapusButton.setEnabled(false);
            tabel.setEnabled(false);
        });
    }

    // Menonaktifkan kontrol selama akses database agar operasi tidak dikirim dua kali.
    private void aturSibuk(boolean nilai) {
        sibuk = nilai;
        namaField.setEnabled(!nilai);
        beratField.setEnabled(!nilai);
        layananBox.setEnabled(!nilai);
        statusBox.setEnabled(!nilai && transaksiTerpilih != null);
        resetButton.setEnabled(!nilai);
        tabel.setEnabled(!nilai);
        tambahButton.setEnabled(!nilai && transaksiTerpilih == null);
        ubahButton.setEnabled(!nilai && transaksiTerpilih != null);
        hapusButton.setEnabled(!nilai && transaksiTerpilih != null);
    }

    // Menjalankan JDBC di latar belakang dan menangani hasil di thread Swing; memori dijalankan langsung.
    private <T> void kerjakan(Supplier<T> operasi, Consumer<T> berhasil, Consumer<RuntimeException> gagal) {
        // Repository memori tidak menunggu I/O; jalankan langsung untuk demo dan pemeriksaan lama.
        if (!(repository instanceof JdbcTransaksiRepository)) {
            T hasil;
            try { hasil = operasi.get(); }
            catch (RuntimeException error) { gagal.accept(error); return; }
            berhasil.accept(hasil);
            return;
        }
        aturSibuk(true);
        pesanLabel.setText("Mengakses database...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() { return operasi.get(); }

            @Override
            protected void done() {
                aturSibuk(false);
                T hasil;
                try { hasil = get(); }
                catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    gagal.accept(new IllegalStateException("Operasi terhenti. Muat ulang daftar.", error));
                    return;
                } catch (ExecutionException error) {
                    Throwable sebab = error.getCause();
                    gagal.accept(sebab instanceof RuntimeException runtime ? runtime
                            : new IllegalStateException("Operasi database gagal.", sebab));
                    return;
                }
                berhasil.accept(hasil);
            }
        }.execute();
    }
}
