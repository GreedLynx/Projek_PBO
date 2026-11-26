package repository;

import model.TransaksiLaundry;

import java.util.List;

// Kontrak CRUD agar GUI dapat menggunakan penyimpanan memori atau JDBC.
public interface TransaksiRepository {
    // Menyimpan transaksi baru dan mengembalikan data dengan ID serta tanggal masuk.
    TransaksiLaundry tambah(TransaksiLaundry transaksi);
    // Mengembalikan salinan daftar transaksi untuk ditampilkan tanpa mengubah koleksi penyimpanan.
    List<TransaksiLaundry> daftar();
    // Menerapkan perubahan sambil mempertahankan identitas, tanggal, dan harga lama jika layanan sama.
    TransaksiLaundry ubah(TransaksiLaundry transaksi);
    // Menghapus berdasarkan ID dan menolak operasi jika transaksi tidak ditemukan.
    void hapus(long id);
}
