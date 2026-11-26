-- Jalankan pada database laundrykos. Tidak menghapus tabel atau data yang sudah ada.
CREATE TABLE transaksi_laundry (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nama_pelanggan VARCHAR(100) NOT NULL
        CHECK (char_length(btrim(nama_pelanggan)) BETWEEN 1 AND 100
            AND nama_pelanggan = btrim(nama_pelanggan)),
    berat_kg NUMERIC(10,2) NOT NULL CHECK (berat_kg > 0 AND berat_kg <= 99999999.99),
    layanan VARCHAR(30) NOT NULL
        CHECK (layanan IN ('Cuci saja', 'Cuci setrika', 'Setrika saja')),
    harga_per_kg NUMERIC(12,2) NOT NULL
        CHECK (harga_per_kg > 0 AND harga_per_kg <= 9999999999.99),
    total_biaya NUMERIC(22,2) NOT NULL
        CHECK (total_biaya > 0 AND total_biaya = round(berat_kg * harga_per_kg, 2)),
    status VARCHAR(20) NOT NULL DEFAULT 'Diproses'
        CHECK (status IN ('Diproses', 'Selesai', 'Diambil')),
    tanggal_masuk TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
