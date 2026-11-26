package repository;

import model.TransaksiLaundry;
import model.Layanan;
import config.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Menjalankan CRUD PostgreSQL melalui kontrak TransaksiRepository; SQL dipisahkan dari GUI.
public class JdbcTransaksiRepository implements TransaksiRepository {
    // Membungkus kegagalan I/O atau SQL menjadi pesan aplikasi dengan penyebab asli.
    private IllegalStateException gagal(Exception error) {
        String detail = error instanceof SQLException sql ? " (SQLState: " + sql.getSQLState() + ")" : "";
        return new IllegalStateException("Akses database gagal" + detail
                + ". Periksa konfigurasi, server, dan driver JDBC.", error);
    }

    // Mengubah satu baris hasil SQL menjadi objek transaksi; total dihitung kembali oleh model.
    private TransaksiLaundry baca(ResultSet hasil) throws SQLException {
        Layanan layanan = null;
        for (Layanan pilihan : Layanan.values()) {
            if (pilihan.toString().equals(hasil.getString("layanan"))) layanan = pilihan;
        }
        return new TransaksiLaundry(hasil.getLong("id"), hasil.getString("nama_pelanggan"),
                hasil.getBigDecimal("berat_kg"), layanan, hasil.getBigDecimal("harga_per_kg"),
                hasil.getString("status"), hasil.getTimestamp("tanggal_masuk").toLocalDateTime());
    }

    @Override
    // Menyimpan transaksi baru dan mengembalikan data dengan ID serta tanggal masuk.
    public TransaksiLaundry tambah(TransaksiLaundry transaksi) {
        Objects.requireNonNull(transaksi, "Transaksi wajib diisi.");
        if (transaksi.getId() != 0 || !transaksi.getStatus().equals("Diproses")) {
            throw new IllegalArgumentException("Tambah memerlukan transaksi baru berstatus Diproses.");
        }
        String sql = "INSERT INTO public.transaksi_laundry "
                + "(nama_pelanggan, berat_kg, layanan, harga_per_kg, total_biaya) VALUES (?, ?, ?, ?, ?) RETURNING *";
        try (Connection koneksi = DatabaseConnection.buka();
                PreparedStatement query = koneksi.prepareStatement(sql)) {
            query.setString(1, transaksi.getNamaPelanggan());
            query.setBigDecimal(2, transaksi.getBeratKg());
            query.setString(3, transaksi.getLayanan().toString());
            query.setBigDecimal(4, transaksi.getHargaPerKg());
            query.setBigDecimal(5, transaksi.getTotalBiaya());
            try (ResultSet hasil = query.executeQuery()) {
                if (!hasil.next()) throw new SQLException("INSERT tidak mengembalikan transaksi.");
                return baca(hasil);
            }
        } catch (IOException | SQLException error) { throw gagal(error); }
    }

    @Override
    // Mengembalikan salinan daftar transaksi untuk ditampilkan tanpa mengubah koleksi penyimpanan.
    public List<TransaksiLaundry> daftar() {
        try (Connection koneksi = DatabaseConnection.buka();
                PreparedStatement query = koneksi.prepareStatement("SELECT * FROM public.transaksi_laundry ORDER BY id");
                ResultSet hasil = query.executeQuery()) {
            List<TransaksiLaundry> daftar = new ArrayList<>();
            while (hasil.next()) daftar.add(baca(hasil));
            return List.copyOf(daftar);
        } catch (IOException | SQLException error) { throw gagal(error); }
    }

    @Override
    // Menerapkan perubahan sambil mempertahankan identitas, tanggal, dan harga lama jika layanan sama.
    public TransaksiLaundry ubah(TransaksiLaundry transaksi) {
        Objects.requireNonNull(transaksi, "Transaksi wajib diisi.");
        // Harga historis dipilih dari record yang terkunci dalam satu UPDATE.
        String sql = "UPDATE public.transaksi_laundry SET nama_pelanggan=?, berat_kg=?, "
                + "harga_per_kg=CASE WHEN layanan=? THEN harga_per_kg ELSE ? END, "
                + "total_biaya=round(? * CASE WHEN layanan=? THEN harga_per_kg ELSE ? END, 2), "
                + "layanan=?, status=? WHERE id=? RETURNING *";
        try (Connection koneksi = DatabaseConnection.buka();
                PreparedStatement query = koneksi.prepareStatement(sql)) {
            query.setString(1, transaksi.getNamaPelanggan());
            query.setBigDecimal(2, transaksi.getBeratKg());
            query.setString(3, transaksi.getLayanan().toString());
            query.setBigDecimal(4, transaksi.getLayanan().getHargaPerKg());
            query.setBigDecimal(5, transaksi.getBeratKg());
            query.setString(6, transaksi.getLayanan().toString());
            query.setBigDecimal(7, transaksi.getLayanan().getHargaPerKg());
            query.setString(8, transaksi.getLayanan().toString());
            query.setString(9, transaksi.getStatus());
            query.setLong(10, transaksi.getId());
            try (ResultSet hasil = query.executeQuery()) {
                if (!hasil.next()) throw new IllegalArgumentException("Transaksi tidak ditemukan. Muat ulang daftar.");
                return baca(hasil);
            }
        } catch (IOException | SQLException error) { throw gagal(error); }
    }

    @Override
    // Menghapus berdasarkan ID dan menolak operasi jika transaksi tidak ditemukan.
    public void hapus(long id) {
        try (Connection koneksi = DatabaseConnection.buka();
                PreparedStatement query = koneksi.prepareStatement("DELETE FROM public.transaksi_laundry WHERE id=?")) {
            query.setLong(1, id);
            if (query.executeUpdate() != 1) {
                throw new IllegalArgumentException("Transaksi tidak ditemukan. Muat ulang daftar.");
            }
        } catch (IOException | SQLException error) { throw gagal(error); }
    }
}
