import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Menyimpan transaksi sementara dalam Map; data hilang ketika aplikasi berakhir.
public class InMemoryTransaksiRepository implements TransaksiRepository {
    private final Map<Long, TransaksiLaundry> data = new LinkedHashMap<>();
    private long idBerikutnya = 1;

    @Override
    // Menyimpan transaksi baru dan mengembalikan data dengan ID serta tanggal masuk.
    public TransaksiLaundry tambah(TransaksiLaundry transaksi) {
        Objects.requireNonNull(transaksi, "Transaksi wajib diisi.");
        if (transaksi.getId() != 0 || !transaksi.getStatus().equals("Diproses")) {
            throw new IllegalArgumentException("Tambah memerlukan transaksi baru berstatus Diproses.");
        }
        if (idBerikutnya == Long.MAX_VALUE) {
            throw new IllegalStateException("Nomor transaksi sudah mencapai batas.");
        }
        TransaksiLaundry tersimpan = transaksi.denganIdentitas(idBerikutnya++, LocalDateTime.now());
        data.put(tersimpan.getId(), tersimpan);
        return tersimpan;
    }

    @Override
    // Mengembalikan salinan daftar transaksi untuk ditampilkan tanpa mengubah koleksi penyimpanan.
    public List<TransaksiLaundry> daftar() {
        return List.copyOf(data.values());
    }

    @Override
    // Menerapkan perubahan sambil mempertahankan identitas, tanggal, dan harga lama jika layanan sama.
    public TransaksiLaundry ubah(TransaksiLaundry transaksi) {
        Objects.requireNonNull(transaksi, "Transaksi wajib diisi.");
        TransaksiLaundry lama = data.get(transaksi.getId());
        if (lama == null) {
            throw new IllegalArgumentException("Transaksi tidak ditemukan. Muat ulang daftar.");
        }
        TransaksiLaundry hasil = lama.ubah(transaksi.getNamaPelanggan(), transaksi.getBeratKg(),
                transaksi.getLayanan(), transaksi.getStatus());
        data.put(hasil.getId(), hasil);
        return hasil;
    }

    @Override
    // Menghapus berdasarkan ID dan menolak operasi jika transaksi tidak ditemukan.
    public void hapus(long id) {
        if (data.remove(id) == null) {
            throw new IllegalArgumentException("Transaksi tidak ditemukan. Muat ulang daftar.");
        }
    }
}
