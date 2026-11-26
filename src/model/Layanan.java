package model;

import java.math.BigDecimal;

// Pilihan layanan beserta nama tampilan dan tarif per kilogram.
public enum Layanan {
    CUCI_SAJA("Cuci saja", "5000"),
    CUCI_SETRIKA("Cuci setrika", "7000"),
    SETRIKA_SAJA("Setrika saja", "4000");

    private final String nama;
    private final BigDecimal hargaPerKg;

    Layanan(String nama, String hargaPerKg) {
        this.nama = nama;
        this.hargaPerKg = new BigDecimal(hargaPerKg);
    }

    // Membaca tarif per kilogram.
    public BigDecimal getHargaPerKg() {
        return hargaPerKg;
    }

    @Override
    // Mengembalikan nama layanan untuk ditampilkan pada combo box dan disimpan di database.
    public String toString() {
        return nama;
    }
}
