# LaundryKos

Aplikasi desktop Java Swing untuk mencatat transaksi laundry dengan penyimpanan PostgreSQL melalui JDBC. Proyek ini menerapkan konsep PBO dan SOLID untuk tugas kuliah. Sasaran awalnya adalah petugas laundry kecil yang perlu mencatat cucian masuk, menghitung biaya, dan memperbarui status pengerjaan.

## 1. Kebutuhan (requirements)

Ketentuan dosen yang disampaikan pengguna: menerapkan SOLID, terhubung ke database, dan sesuai proses bisnis di lapangan. Presentasi berupa demo kepada teman kelas untuk mendapatkan masukan menuju MVP atau produk layak jual. Kelayakan penggunaan nyata masih perlu divalidasi kepada pelaku usaha laundry.

### Kebutuhan perangkat lunak

- JDK 21 sebagai acuan lingkungan proyek; `java` dan `javac` tersedia pada PATH.
- PostgreSQL yang berjalan; lingkungan evaluasi sebelumnya menggunakan PostgreSQL 15.
- Driver PostgreSQL JDBC `42.7.14`, disediakan di `lib/`.
- Desktop untuk menampilkan GUI Swing.
- DBeaver atau pgAdmin dapat digunakan untuk menjalankan skrip SQL; keduanya bukan dependensi aplikasi.

Proyek menggunakan pustaka standar Java dan driver JDBC, tanpa Maven, Gradle, atau framework aplikasi.

### Kebutuhan fungsional

- Tambah, lihat, pilih, ubah, dan hapus transaksi.
- Perhitungan harga dan total otomatis.
- Validasi nama, berat, layanan, dan status.
- Konfirmasi sebelum menghapus serta Reset untuk kembali ke mode tambah.
- Penyimpanan permanen dan pemuatan ulang data dari PostgreSQL.
- Pesan kegagalan dan kontrol yang dinonaktifkan selama akses database.

## 2. Proses bisnis dan aturan data

1. Petugas menerima cucian, menimbangnya, lalu mengisi nama pelanggan dan memilih layanan.
2. Aplikasi menghitung total dari berat dikali harga per kilogram.
3. Petugas menyimpan transaksi; ID dan tanggal masuk dibuat otomatis, dengan status awal **Diproses**.
4. Petugas memilih transaksi untuk memperbarui data atau status menjadi **Selesai** atau **Diambil**.
5. Data tetap tersimpan setelah aplikasi ditutup.

Status menggambarkan tahap pekerjaan, tetapi kode belum membatasi urutan perpindahan status. Penimbangan dan pengerjaan cucian dilakukan di luar aplikasi.

| Layanan | Tarif per kg |
| --- | ---: |
| Cuci saja | Rp5.000 |
| Cuci setrika | Rp7.000 |
| Setrika saja | Rp4.000 |

Tarif tersebut adalah keputusan proyek, bukan standar harga usaha laundry.

- Nama wajib diisi, dipangkas spasi tepinya, dan maksimal 100 karakter Unicode.
- Berat harus positif, maksimal `99999999.99` kg, dengan maksimal dua desimal. Input menerima `2,5` atau `2.5`, tanpa pemisah ribuan.
- Tidak ada minimum kilogram. Contoh: 2,5 kg Cuci setrika menghasilkan Rp17.500.
- Perhitungan memakai `BigDecimal`; total dibulatkan dua desimal dengan `HALF_UP`.
- Setiap transaksi memiliki satu layanan; nama pelanggan yang sama boleh memiliki beberapa transaksi.
- Harga per kg disimpan sebagai harga historis. Jika layanan tetap sama saat diedit, harga lama dipertahankan. Jika layanan berubah, tarif layanan baru digunakan.
- ID dan tanggal masuk tetap saat diedit. Penghapusan menghapus record secara permanen.

## 3. Struktur folder

```text
PBO12.1/
|-- src/
|   |-- Main.java
|   |-- ui/LaundryFrame.java
|   |-- service/LaundryService.java
|   |-- model/ (TransaksiLaundry.java, Layanan.java)
|   |-- repository/ (TransaksiRepository.java,
|   |                  InMemoryTransaksiRepository.java, JdbcTransaksiRepository.java)
|   +-- config/DatabaseConnection.java
|-- test/          # Program pemeriksaan
|-- config/        # File konfigurasi database lokal dan contohnya
|-- sql/           # Skrip pembuatan database dan tabel
|-- lib/           # Driver JDBC
|-- out/           # Hasil kompilasi
|-- docs/          # Pedoman dan dokumentasi
|-- deliverables/  # Paket/artefak terdahulu
+-- .vscode/       # Pengaturan editor
```

Package ui menangani tampilan, service validasi/perhitungan, model objek data, repository penyimpanan, dan config pembukaan koneksi. Main dan kelas pemeriksaan tetap tanpa package.

### Tanggung jawab kelas

| File | Fungsi |
| --- | --- |
| `Main.java` | Titik masuk; memilih repository JDBC dan membuka GUI melalui `SwingUtilities.invokeLater`. |
| `LaundryFrame.java` | Menyusun form/tabel, menangani event, memanggil repository, dan memperbarui tampilan. |
| `LaundryService.java` | Metode statis untuk validasi nama/berat serta perhitungan total; tidak mengatur CRUD. |
| `Layanan.java` | Enum pilihan layanan, nama tampilan, dan tarif per kilogram. |
| `TransaksiLaundry.java` | Model immutable satu transaksi; validasi saat konstruksi dan pembuatan salinan saat perubahan. |
| `TransaksiRepository.java` | Kontrak `tambah`, `daftar`, `ubah`, dan `hapus`. |
| `InMemoryTransaksiRepository.java` | Implementasi CRUD dengan `LinkedHashMap`; data hanya selama proses berjalan. |
| `JdbcTransaksiRepository.java` | Implementasi CRUD PostgreSQL menggunakan SQL dan JDBC. |
| `DatabaseConnection.java` | Membaca konfigurasi lokal dan membuka koneksi JDBC. |

## 4. Penyiapan database

Jalankan perintah terminal dari folder proyek yang berisi `src`, `config`, `sql`, dan `lib`.

1. Sambungkan DBeaver/pgAdmin ke database `postgres`, lalu jalankan [01_create_database.sql](sql/01_create_database.sql) satu kali untuk membuat `laundrykos`. Lewati jika database sudah ada.
2. Sambungkan ke database `laundrykos`, lalu jalankan [02_create_table.sql](sql/02_create_table.sql). Jika tabel sudah ada, periksa struktur yang tersedia; jangan menghapus data untuk menyiapkannya kembali.
3. Buat konfigurasi lokal jika belum ada:

```powershell
Copy-Item config/database.properties.example config/database.properties
```

Jangan menimpa konfigurasi yang sudah diisi. Sesuaikan isinya dengan server lokal:

```properties
db.url=jdbc:postgresql://localhost:5432/laundrykos
db.user=postgres
db.password=ISI_PASSWORD_LOKAL
```

Ganti placeholder password tanpa tanda kutip. Pada format Java properties, backslash dalam password ditulis dua kali. Jangan membagikan konfigurasi berisi kredensial.

Tabel `public.transaksi_laundry` menyimpan ID, nama pelanggan, berat, layanan, harga per kg, total biaya, status, dan tanggal masuk. Detail tipe serta constraint berada pada skrip SQL. Aplikasi tidak otomatis membuat database atau tabel.

## 5. Kompilasi dan menjalankan

Perintah berikut untuk PowerShell di Windows:

```powershell
javac -encoding UTF-8 -d out -sourcepath src src/Main.java test/*.java
java -cp "out;lib/*" DatabaseConnectionCheck
java -cp "out;lib/*" Main
```

Jika kompilasi gagal, selesaikan error sebelum menjalankan aplikasi. Pemeriksaan koneksi yang berhasil menunjukkan database dan tabel tersedia. `Main` membuka GUI dan memuat transaksi yang tersimpan. Pada database baru, tabel GUI kosong.

Untuk kompilasi aplikasi saja tanpa folder test, gunakan:

```powershell
javac -encoding UTF-8 -d out -sourcepath src src/Main.java
```

Jika source belum berubah setelah kompilasi, cukup jalankan perintah `Main`. Folder `test` tidak otomatis dijalankan saat aplikasi dibuka.

## 6. Alur eksekusi berdasarkan kode

```text
Main.main()
  → SwingUtilities.invokeLater(...)
  → new JdbcTransaksiRepository()
  → new LaundryFrame(repository)
  → susun komponen dan pasang listener
  → resetForm() dan muatUlang("")
  → repository.daftar() lewat SwingWorker
  → DatabaseConnection.buka() → PostgreSQL
  → tampilkanTabel(...) pada thread Swing
  → menunggu tindakan pengguna
```

### Tambah atau ubah

1. Listener tombol memanggil `LaundryFrame.simpanTransaksi()`.
2. `LaundryService.bacaBerat()` mengubah input menjadi angka tervalidasi.
3. GUI membuat `TransaksiLaundry` baru atau memanggil `transaksiTerpilih.ubah(...)`. Model menggunakan validasi dan perhitungan statis `LaundryService`.
4. `kerjakan(...)` menjalankan `repository.tambah(...)` atau `repository.ubah(...)` di latar belakang untuk JDBC.
5. Repository membuka koneksi, mengisi parameter `PreparedStatement`, dan mengeksekusi SQL. `RETURNING` mengambil record hasil penyimpanan.
6. Setelah berhasil, `selesaiOperasi()` mereset form dan memuat ulang daftar. Kegagalan ditampilkan pada label pesan.

GUI memanggil repository secara langsung untuk CRUD. Tidak ada objek service perantara CRUD yang dibuat oleh `Main`.

### Pilih, hapus, reset, dan tutup

- Pilih baris: `pilihTransaksi()` mengambil objek sesuai indeks model tabel dan mengisi form; mode ubah/hapus aktif.
- Hapus: `hapusTransaksi()` meminta konfirmasi, lalu menghapus berdasarkan ID melalui repository dan memuat ulang daftar.
- Reset: membersihkan isian/pilihan dan memuat ulang data; perubahan form yang belum disimpan dibatalkan.
- Tutup jendela: proses aplikasi berakhir melalui `EXIT_ON_CLOSE`; data PostgreSQL tetap tersimpan.

`SwingWorker.doInBackground()` mengerjakan akses database, sedangkan `done()` menangani hasil pada thread Swing. Kontrol dinonaktifkan selama operasi untuk mencegah pengiriman ganda. Repository memori dijalankan langsung oleh GUI.

## 7. Penerapan SOLID

| Prinsip | Bukti dalam kode | Penjelasan dan batas penerapan |
| --- | --- | --- |
| **SRP — Single Responsibility** | `LaundryFrame`, `LaundryService`, `DatabaseConnection`, repository | Tampilan, validasi/perhitungan, pembukaan koneksi, dan SQL dipisahkan menurut tanggung jawab. GUI tetap mengoordinasikan alur interaksi. |
| **OCP — Open/Closed** | `TransaksiRepository` dengan implementasi memori dan JDBC | Penyimpanan dapat diperluas melalui implementasi kontrak. Belum sepenuhnya independen: GUI memeriksa `instanceof JdbcTransaksiRepository` untuk pekerjaan latar belakang dan label penyimpanan. |
| **LSP — Liskov Substitution** | Kedua implementasi `TransaksiRepository` | Keduanya menerima kontrak CRUD yang sama: tambah menghasilkan identitas, ubah mempertahankan identitas/tanggal, dan record hilang ditolak. Kemampuan saling menggantikan tetap bergantung pada konsistensi perilaku tersebut; JDBC juga dapat gagal karena I/O. |
| **ISP — Interface Segregation** | Empat metode `TransaksiRepository` | Kontrak fokus pada kebutuhan CRUD transaksi, tanpa memaksakan fungsi pembayaran, laporan, atau pencetakan. |
| **DIP — Dependency Inversion** | `LaundryFrame(TransaksiRepository repository)` | GUI menerima abstraksi melalui konstruktor; `Main` memilih implementasinya. Penerapan masih sebagian karena pemeriksaan kelas JDBC di GUI. |

Contoh penghubung implementasi dengan abstraksi:

```java
// Main memilih penyimpanan konkret.
new LaundryFrame(new JdbcTransaksiRepository()).setVisible(true);

// LaundryFrame menerima kontrak penyimpanan.
public LaundryFrame(TransaksiRepository repository)
```

Contoh kedua adalah potongan deklarasi konstruktor, bukan program lengkap untuk dikompilasi tersendiri.

## 8. Konsep PBO dan teknik Java lainnya

| Konsep | Contoh |
| --- | --- |
| Enkapsulasi | Atribut `private final` dan getter pada `TransaksiLaundry`; konstruksi objek memvalidasi data. |
| Immutability | `ubah()` dan `denganIdentitas()` menghasilkan objek transaksi baru. |
| Inheritance | `LaundryFrame extends JFrame`. |
| Abstraksi/interface | `TransaksiRepository` mendefinisikan kemampuan penyimpanan. |
| Polimorfisme | Pemanggilan CRUD melalui variabel bertipe `TransaksiRepository` menggunakan implementasi objeknya. |
| Enum | `Layanan` membatasi pilihan layanan beserta tarif. |
| Collections | `LinkedHashMap` untuk penyimpanan memori; `List` untuk daftar transaksi. |
| Event Swing | Listener menghubungkan tombol, input berat, pilihan layanan, dan pilihan baris dengan tindakan GUI. |
| Exception handling | Input salah dan kegagalan SQL/I/O diteruskan menjadi pesan yang dapat ditindaklanjuti. |
| Pengelolaan resource | `try-with-resources` menutup koneksi, statement, dan result set. |
| Parameter SQL | `PreparedStatement` memisahkan masukan pengguna dari teks perintah SQL. |

## 9. Pemeriksaan yang tersedia

Pemeriksaan berikut merupakan program Java terpisah. Gunakan `-ea` agar pemeriksaan `assert` aktif.

| File | Cakupan utama |
| --- | --- |
| `LaundryServiceCheck` | Validasi dan perhitungan biaya. |
| `RepositoryCheck` | CRUD memori, identitas, tanggal, dan harga historis. |
| `DatabaseConfigCheck` | Konfigurasi koneksi. |
| `DatabaseConnectionCheck` | Koneksi nyata dan keberadaan tabel. |
| `LayoutCheck` | Layout GUI, input, perhitungan, dan Reset. |
| `CrudGuiCheck` | Alur CRUD GUI dengan penyimpanan memori. |
| `JdbcRepositoryCheck` | Operasi repository PostgreSQL dan persistensi. |
| `JdbcGuiCheck` | Integrasi GUI dengan repository JDBC. |
| `Minggu2Check` | Skenario penerimaan GUI PostgreSQL dan pemulihan kegagalan koneksi. |

Contoh pemeriksaan logika tanpa server database:

```powershell
java -ea -cp out LaundryServiceCheck
java -ea -cp out RepositoryCheck
java -ea -cp out DatabaseConfigCheck
```

Pemeriksaan GUI memerlukan desktop. Pemeriksaan JDBC memerlukan konfigurasi dan PostgreSQL; pemeriksaan integrasi dapat membuat lalu membersihkan data uji, sehingga nomor identity tetap bertambah. Daftar ini menjelaskan pemeriksaan yang tersedia, bukan klaim bahwa semuanya dijalankan ulang saat README dibuat.

## 10. Penanganan masalah

| Masalah | Langkah |
| --- | --- |
| `java` atau `javac` tidak dikenali | Periksa pemasangan JDK dan PATH terminal. |
| `Main` tidak ditemukan | Kompilasi dari folder proyek dan gunakan classpath `out;lib/*`. |
| Driver JDBC tidak ditemukan | Pastikan JAR ada di `lib/` dan classpath menyertakan `lib/*`. |
| Konfigurasi tidak ditemukan/tidak valid | Buat konfigurasi lokal dan ganti placeholder dengan nilai server sendiri. |
| Koneksi atau autentikasi gagal | Periksa server, port, nama database, username, dan password. |
| Tabel belum tersedia | Jalankan skrip tabel pada database `laundrykos`. |
| Daftar gagal dimuat | Perbaiki masalah lalu tekan Reset untuk memuat ulang. |

Jika penyimpanan berhasil tetapi pemuatan daftar gagal, gunakan Reset untuk memeriksa data terbaru sebelum menambah kembali transaksi yang sama.

## 11. Batas versi dan bahan evaluasi MVP

Versi ini berfokus pada pencatatan transaksi. Login, hak akses, pembayaran, cetak struk, laporan keuangan, dan banyak cabang belum tersedia. Belum ada bukti bahwa aplikasi sudah layak jual hanya karena CRUD dan database berfungsi.

Pertanyaan untuk diskusi kelas dan calon pengguna:

- Apakah data transaksi cukup untuk mengenali pelanggan dan cucian yang akan diambil?
- Apakah perlu nomor telepon, estimasi selesai, atau bukti penerimaan?
- Apakah tarif, pembulatan, dan aturan status sesuai usaha yang dituju?
- Fitur apa yang paling menghambat penggunaan jika belum tersedia?

Masukan menjadi backlog untuk diprioritaskan; penambahan fitur dilakukan setelah lingkup disepakati. Dokumentasi pendamping dan rencana proyek berada di `docs/`.
