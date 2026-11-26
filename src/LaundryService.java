import java.math.BigDecimal;

// Menyediakan validasi dan perhitungan statis yang digunakan GUI serta model transaksi.
public class LaundryService {
    private static final BigDecimal BERAT_MAKSIMAL = new BigDecimal("99999999.99");

    // Memangkas spasi tepi dan memastikan nama terisi serta maksimal 100 karakter Unicode.
    public static String validasiNama(String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama pelanggan wajib diisi.");
        }
        String hasil = nama.trim();
        if (hasil.codePointCount(0, hasil.length()) > 100) {
            throw new IllegalArgumentException("Nama pelanggan maksimal 100 karakter.");
        }
        return hasil;
    }

    // Mengubah input koma atau titik menjadi BigDecimal setelah memeriksa format maksimal dua desimal.
    public static BigDecimal bacaBerat(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Berat wajib diisi.");
        }
        String teks = input.trim();
        if (!teks.matches("[0-9]+([.,][0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    "Berat harus angka positif, maksimal 2 desimal, tanpa pemisah ribuan.");
        }
        return validasiBerat(new BigDecimal(teks.replace(',', '.')));
    }

    // Memastikan berat positif, maksimal dua desimal, dan sesuai kapasitas kolom database.
    public static BigDecimal validasiBerat(BigDecimal berat) {
        if (berat == null || berat.signum() <= 0 || berat.compareTo(BERAT_MAKSIMAL) > 0) {
            throw new IllegalArgumentException("Berat harus lebih dari 0 dan maksimal 99999999,99 kg.");
        }
        if (berat.scale() > 2) {
            throw new IllegalArgumentException("Berat maksimal 2 angka desimal.");
        }
        return berat;
    }

    // Memvalidasi berat dan harga lalu menghitung total dengan pembulatan dua desimal.
    public static BigDecimal hitungTotal(BigDecimal berat, BigDecimal hargaPerKg) {
        validasiBerat(berat);
        if (hargaPerKg == null || hargaPerKg.signum() <= 0
                || hargaPerKg.compareTo(new BigDecimal("9999999999.99")) > 0
                || hargaPerKg.scale() > 2) {
            throw new IllegalArgumentException("Harga per kg harus positif dan sesuai rentang database.");
        }
        // Total disimpan dua desimal; pecahan sen dibulatkan HALF_UP.
        return berat.multiply(hargaPerKg).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
