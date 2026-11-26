import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Data transaksi yang tidak diubah langsung: perubahan menghasilkan objek baru yang tervalidasi.
public class TransaksiLaundry {
    private final long id;
    private final LocalDateTime tanggalMasuk;
    private final String namaPelanggan;
    private final BigDecimal beratKg;
    private final Layanan layanan;
    private final BigDecimal hargaPerKg;
    private final BigDecimal totalBiaya;
    private final String status;

    // Membangun transaksi dan memeriksa aturan data sebelum objek dapat digunakan.
    public TransaksiLaundry(String namaPelanggan, BigDecimal beratKg, Layanan layanan) {
        this(0, namaPelanggan, beratKg, layanan,
                layanan == null ? null : layanan.getHargaPerKg(), "Diproses", null);
    }

    // ID 0 dan tanggal null menandakan transaksi belum disimpan.
    // Membangun transaksi dan memeriksa aturan data sebelum objek dapat digunakan.
    public TransaksiLaundry(long id, String namaPelanggan, BigDecimal beratKg, Layanan layanan,
            BigDecimal hargaPerKg, String status, LocalDateTime tanggalMasuk) {
        if (id < 0 || (id == 0) != (tanggalMasuk == null)) {
            throw new IllegalArgumentException("ID dan tanggal transaksi tidak sesuai.");
        }
        if (!List.of("Diproses", "Selesai", "Diambil").contains(status == null ? "" : status)) {
            throw new IllegalArgumentException("Status transaksi tidak valid.");
        }
        this.id = id;
        this.tanggalMasuk = tanggalMasuk;
        this.namaPelanggan = LaundryService.validasiNama(namaPelanggan);
        this.beratKg = LaundryService.validasiBerat(beratKg);
        if (layanan == null) {
            throw new IllegalArgumentException("Layanan wajib dipilih.");
        }
        this.layanan = layanan;
        this.hargaPerKg = hargaPerKg;
        this.totalBiaya = LaundryService.hitungTotal(this.beratKg, this.hargaPerKg);
        this.status = status;
    }

    // Menerapkan perubahan sambil mempertahankan identitas, tanggal, dan harga lama jika layanan sama.
    public TransaksiLaundry ubah(String nama, BigDecimal berat, Layanan layananBaru, String statusBaru) {
        BigDecimal harga = layananBaru == layanan ? hargaPerKg
                : layananBaru == null ? null : layananBaru.getHargaPerKg();
        return new TransaksiLaundry(id, nama, berat, layananBaru, harga, statusBaru, tanggalMasuk);
    }

    // Membuat salinan transaksi dengan ID dan tanggal yang diberikan repository.
    public TransaksiLaundry denganIdentitas(long id, LocalDateTime tanggal) {
        return new TransaksiLaundry(id, namaPelanggan, beratKg, layanan, hargaPerKg, status, tanggal);
    }

    // Membaca id tanpa mengubah transaksi.
    public long getId() { return id; }
    // Membaca tanggal masuk tanpa mengubah transaksi.
    public LocalDateTime getTanggalMasuk() { return tanggalMasuk; }

    // Membaca nama pelanggan tanpa mengubah transaksi.
    public String getNamaPelanggan() { return namaPelanggan; }
    // Membaca berat kg tanpa mengubah transaksi.
    public BigDecimal getBeratKg() { return beratKg; }
    // Membaca layanan tanpa mengubah transaksi.
    public Layanan getLayanan() { return layanan; }
    // Membaca tarif per kilogram.
    public BigDecimal getHargaPerKg() { return hargaPerKg; }
    // Membaca total biaya tanpa mengubah transaksi.
    public BigDecimal getTotalBiaya() { return totalBiaya; }
    // Membaca status tanpa mengubah transaksi.
    public String getStatus() { return status; }
}
