# iTalase — Ide & Arah Produk

**iTalase** adalah aplikasi manajemen stok barang dan kasir (*Point of Sale*)
untuk warung dan toko kecil. Ditujukan untuk **performa tinggi, efisiensi operasional, dan keandalan
transaksi secara *offline-first***: input barang, pencatatan transaksi, dan laporan harian semuanya
berjalan tanpa internet.

---

## Arsitektur & Teknologi Utama

* **Kotlin Multiplatform (KMP):** Berbagi *business logic*, data layer, dan manajemen status secara
  penuh antara iOS dan Android — konsistensi fitur dan efisiensi pemeliharaan kode.
* **Compose Multiplatform:** UI deklaratif dan *native* di kedua platform dari satu basis kode.
* **Room KMP + SQLCipher (Offline-First & Terenkripsi):** Seluruh data barang, transaksi, dan laporan
  tersimpan lokal di perangkat dan terenkripsi *at-rest* via SQLCipher 4. Kunci dikelola aman
  lewat platform keystore (Android Keystore dan iOS Keychain), dengan migrasi otomatis dari database
  polos lama tanpa kehilangan data.
* **Animasi & Reaktivitas Scroll Terpadu:** Sistem top app bar adaptif `Liquid Glass` dengan
  `Modifier.nestedScrollConnection` di seluruh layar. Dilengkapi ticker angka terisolasi (*directional number ticker*)
  berbasis pegas (*spring physics*) yang menggeser angka naik/turun sesuai pertambahan/pengurangan tanpa
  memicu *jitter* atau *layout jump* pada label teks.
* **Generator PDF Mandiri:** Pembuat berkas PDF murni (*zero-dependency*) dalam Kotlin untuk ekspor
  laporan riwayat penjualan tanpa dependensi pustaka luar.
* **Koin + Coroutines/Flow:** Injeksi dependensi dan aliran data reaktif dari database ke layar.
* **Tooling & Compose Preview Deklaratif:** Penyiapan fungsi `@Preview` di modul `:shared` yang dirender
  langsung melalui Android CLI (`android preview render`) untuk inspeksi visual instan tanpa emulator.
* **Pipeline Rilis & Keamanan Build:** Dukungan otomatisasi penandatanganan rilis (*release signing*)
  dinamis via `local.properties`, optimasi R8/ProGuard minification & bytecode shrinking, serta pembuatan
  paket produksi Android App Bundle (AAB) bertanda tangan valid untuk distribusi Google Play Store.
* **Versi pustaka terkini** dipusatkan di `gradle/libs.versions.toml`.

---

## Bahasa Desain

Aplikasi memakai satu bahasa visual yang konsisten: **Liquid Glass × Glassmorphism × Material3
Expressive** — permukaan kaca berlapis dengan kedalaman (specular sheen + shadow bertint), **tanpa
garis tepi** (`BorderStroke` adalah penanda paling kentara kartu Material murah).

* **Resep Permukaan Tunggal:** `Modifier.glassSurface()`. Kartu, baris, kolom input,
  chip, tombol, panel, dan dialog semuanya turunan resep itu.
* **Top App Bar Liquid Glass:** Header dinamis yang merespons fraksi kolaps scroll (`scrollBehavior.state.collapsedFraction`),
  meningkatkan elevasi kaca, translusensi, dan specular sheen saat konten digulir ke atas.
* **Ticker Angka Directional Terisolasi:** Eyebrow bar memisahkan prefix, nilai numerik, dan suffix
  menggunakan regex `(\D*)(\d+)(.*)`. Hanya nilai integer yang bertransisi vertikal secara dinamis
  (meluncur naik saat bertambah, meluncur turun saat berkurang), menjaga label teks tetap tenang dan stabil.
* **FAB Liquid Glass M3 Expressive:** Tombol aksi mengambang (`PrimaryFab`) dengan elevasi kaca,
  efek tekan pegas (*spring press-scale*), dan kontras ikon tajam yang adaptif terhadap system bars edge-to-edge.
* **Aturan Blur Hemat Daya:** Blur hanya di overlay statis (bottom bar, latar dialog). Konten yang bergulir
  tidak di-blur agar *frame budget* tetap aman di 60/120 fps pada perangkat kelas menengah (*mid-range*).
* **Tipografi & Ikon:** Noto Sans (4 berat statis). Ikon menggunakan Google Material Symbols sebagai
  vector drawable lokal (~1 KB per berkas), bebas dari *overhead* pustaka font ikon besar.
* **Animasi Masuk Terkoordinasi:** `rememberAppear()` + `Modifier.enter(appeared, index)` memberikan transisi
  fade + naik bertahap yang seragam di seluruh layar.

---

## Fitur

* **Manajemen Stok & Inventaris**
    * Pencatatan produk lengkap: nama, barcode, kategori, harga beli/jual, jumlah stok, stok
      minimum, dan satuan.
    * Barang yang habis selalu diurutkan ke bawah; tiap kelompok tetap urut abjad sehingga barang
      yang tersedia mudah ditemukan.
    * Peringatan stok menipis (*low stock alert*) di Beranda.
    * Top bar reaktif dengan ticker jumlah produk aktif per saringan kategori.

* **Sistem Kasir (Point of Sale)**
    * Antarmuka kasir cepat: cari (nama/barcode/kategori), saring per kategori, susun keranjang,
      lalu bayar.
    * Ticker status keranjang interaktif pada top bar eyebrow: menampilkan jumlah item aktif
      (`X BARANG DI KERANJANG`) saat keranjang terisi, atau beralih ke total inventaris (`X BARANG TERCATAT`)
      saat keranjang kosong.
    * Metode bayar: **Tunai, Transfer, atau Bon** (utang pelanggan).
    * Stok berkurang otomatis dalam **satu transaksi database**, mencegah inkonsistensi data.
    * Penghitungan kembalian otomatis saat nominal diterima dimasukkan.

* **Keandalan Akses Offline & Keamanan Data**
    * Seluruh data tersimpan lokal via Room DB; operasional harian kasir tidak membutuhkan jaringan
      sama sekali.
    * Database terenkripsi penuh *at-rest* via SQLCipher 4 (`sqlcipher-android` di Android dan CocoaPods
      SQLCipher di iOS). Kunci dihasilkan dan dilindungi secara perangkat keras (Keystore / Keychain).
    * Migrasi transparan: database versi lama yang belum terenkripsi akan dienkripsi otomatis saat
      pembaruan pertama tanpa intervensi manual.
    * Skema v4 mengindeks kolom pencarian dan relasi kunci asing (`dateMillis`, `saleId`, `category`,
      `debtId`, `status`) demi query yang instan.

* **Riwayat & Laporan**
    * Riwayat transaksi dengan saring kategori dan tampilan detail struk.
    * **Ekspor Laporan Penjualan ke PDF**: Laporan riwayat transaksi dapat diekspor langsung ke berkas PDF
      dengan filter rentang tanggal (Hari Ini, 7 Hari Terakhir, 30 Hari Terakhir, Bulan Ini, atau Rentang
      Kustom) dan dibagikan lewat *system share sheet* (Android Intent / iOS `UIActivityViewController`).
    * Ringkasan harian di Beranda: omzet, laba, jumlah transaksi, pengeluaran, dan pendapatan bersih.
    * Daftar barang terlaris dan barang yang perlu segera ditambah.

* **Bon & Pengeluaran**
    * Utang pelanggan bisa dicatat manual atau otomatis dari penjualan yang belum lunas, lengkap
      dengan pembayaran cicilan dan progresnya.
    * Pengeluaran operasional harian ikut diperhitungkan dalam laba bersih.

* **Pusat Menu & Utilitas (Lainnya)**
    * Kartu kaca terstruktur untuk navigasi cepat ke *Riwayat Transaksi* dan *Pencatatan Pengeluaran*.
    * Kartu status *Keamanan Database* (verifikasi enkripsi SQLCipher dan integritas keystore).
    * Kartu indikator *Status Akses Offline* (penegasan bahwa seluruh operasi berjalan lokal tanpa cloud).
    * Kartu *Info Aplikasi & Lisensi* (versi rilis, build code, dan lisensi tipografi/ikon).
    * Dynamic eyebrow counter yang menampilkan jumlah opsi menu aktif (`5 MENU TERSEDIA`).

* **Aksesibilitas & Navigasi**
    * Lima tab bawah: Beranda, Kasir, Stok, Bon, Lainnya.
    * **Tombol back eksplisit** di Riwayat, Pengeluaran, dan Layar Detail Penjualan demi navigasi
      sempurna di iOS yang tidak memiliki tombol kembali sistem fisik.

---

## Status Fitur

### Sudah Selesai (Production Ready)
* Seluruh fitur bisnis, inventaris, kasir, bon, pengeluaran, dan ekspor laporan PDF.
* Enkripsi database SQLCipher 4 dengan manajemen kunci platform hardware.
* Bahasa desain Liquid Glass × Glassmorphism × Material3 Expressive di seluruh kartu, tombol, dialog, dan FAB.
* Top app bar reaktif scroll dengan efek liquid glass docking dan ticker angka directional terisolasi di semua layar.
* Integrasi visual `@Preview` Compose Multiplatform menggunakan Android CLI sidecar.
* Konfigurasi penandatanganan rilis (*release signing*) aman via `local.properties`.
* Minifikasi dan optimasi bytecode R8/ProGuard pada build release Android.
* Pembuatan Android App Bundle (AAB) produksi yang terverifikasi (`androidApp-release.aab`).
* Pengujian instalasi dan eksekusi pada perangkat fisik Android (Samsung Galaxy A54).

### Rencana Pengembangan Berikutnya (Roadmap)
* **Kesiapan Distribusi Google Play Store:**
    * Penyiapan Google Play Console Closed Testing track (tahap pengujian tertutup 14 hari dengan minimal 20 tester).
    * Pembuatan halaman kebijakan privasi publik (*Privacy Policy*) yang menegaskan model *zero tracking* & *offline-first*.
    * Pembuatan aset visual Play Store: App Icon (512×512 px), Feature Graphic (1024×500 px), dan tangkapan layar perangkat beresolusi tinggi.
* **Pemindaian Barcode via Kamera:** Integrasi CameraX / Google Code Scanner (ML Kit) untuk membaca barcode fisik tanpa perangkat scanner eksternal.
* **Diskon & Promosi:** Dukungan diskon persentase maupun nominal potongan harga tetap per item atau total keranjang.
* **Pembatalan / Void Transaksi:** Fitur pembatalan transaksi yang sudah tersimpan dengan audit log alasan pembatalan dan pengembalian stok otomatis.
* **Dashboard Analitik & Agregasi Bulanan:** Visualisasi grafik tren penjualan harian, mingguan, dan bulanan di Beranda.
* **Pencatatan Pembelian / Kulakan:** Pencatatan transaksi pembelian barang dari pemasok (*purchase orders*) untuk otomatisasi penambahan stok inventaris.

---

## Prinsip Pengembangan

* **Satu sumber kebenaran.** Aturan yang berlaku lintas layar (urutan produk, ukuran halaman, layar
  mana yang punya tombol back) diletakkan di satu tempat dan diuji — bukan disalin ke tiap layar.
* **Satu bahasa visual.** Komponen baru harus memakai primitif kaca yang sudah ada (`Modifier.glassSurface()`),
  bukan membuat gaya kartu/dialog ketiga.
* **Kerjakan hanya yang berubah.** Data yang tidak berubah saat pengguna mengetik tidak dihitung
  ulang saat pengguna mengetik. Animasi angka hanya menggerakkan digit yang berganti, bukan merender ulang seluruh teks bar atas.
* **Ukur, jangan mengklaim.** Optimasi, performa frame rate, dan perbaikan kontras disertai angka, tes,
  dan build artifact yang dapat diverifikasi ulang.
