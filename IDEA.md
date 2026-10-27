# iTalase — Ide & Arah Produk

**iTalase** adalah aplikasi manajemen stok barang dan kasir (*Point of Sale*)
untuk warung dan toko kecil. Ditujukan untuk **performa tinggi, efisiensi operasional, dan keandalan
transaksi secara *offline-first***: input barang, pencatatan transaksi, dan laporan harian semuanya
berjalan tanpa internet.

---

## Arsitektur & Teknologi Utama

* **Kotlin Multiplatform (KMP):** Berbagi *business logic*, data layer, dan manajemen status secara
  penuh antara iOS dan Android — konsistensi fitur dan efisiensi pemeliharaan kode.
* **Compose Multiplatform & Responsivitas Orientasi:** UI deklaratif dan *native* di kedua platform dari satu basis kode.
  Mendukung orientasi tegak (*Portrait*) dan mendatar (*Landscape*) secara adaptif berbasis `BoxWithConstraints`:
  - Mode Kasir Landscape: Tata letak *Master-Detail Split Pane POS* (Katalog produk di kiri 55%, panel keranjang interaktif di kanan 45%).
  - Mode Beranda Landscape: *Dual-Pane Grid* yang membagi KPI omzet/laba di sisi kiri dan peringkat penjualan/stok di sisi kanan.
  - Dock Navigasi Kaca: `LiquidGlassTabBar` memusat dengan batas lebar 560dp melayang menyerupai dock iPad/macOS.
  - Dialog Responsif Keyboard: Seluruh pop-up `GlassDialog` dilengkapi container vertikal scrollable yang mencegah tombol aksi terpotong saat orientasi horizontal atau saat keyboard virtual aktif.
* **Room KMP + SQLCipher (Offline-First & Terenkripsi):** Seluruh data barang, transaksi, dan laporan
  tersimpan lokal di perangkat dan terenkripsi *at-rest* via SQLCipher 4. Kunci dikelola aman
  lewat platform keystore (Android Keystore dan iOS Keychain), dengan migrasi otomatis dari database
  polos lama tanpa kehilangan data.
* **Animasi & Reaktivitas Scroll Terpadu:** Sistem top app bar adaptif `Liquid Glass` dengan
  `Modifier.nestedScrollConnection` di seluruh layar. Dilengkapi ticker angka terisolasi (*directional number ticker*)
  berbasis pegas (*spring physics*) yang menggeser angka naik/turun sesuai pertambahan/pengurangan tanpa
  memicu *jitter* atau *layout jump* pada label teks.
* **Generator PDF Mandiri & Aman:** Pembuat berkas PDF murni (*zero-dependency*) dalam Kotlin untuk ekspor
  laporan riwayat penjualan tanpa dependensi pustaka luar. Dilengkapi sanitasi nama berkas anti-traversal (`../`), pembersihan otomatis cache kadaluarsa (>24 jam), dan izin akses URI aman via `ClipData`.
* **Koin + Coroutines/Flow:** Injeksi dependensi dan aliran data reaktif dari database ke layar.
* **Tooling & Compose Preview Deklaratif:** Penyiapan fungsi `@Preview` di modul `:shared` yang dirender
  langsung melalui Android CLI (`android preview render`) untuk inspeksi visual instan tanpa emulator.
* **Pipeline Rilis & Keamanan Build:** Dukungan otomatisasi penandatanganan rilis (*release signing*)
  dinamis via `local.properties`, optimasi R8/ProGuard minification & bytecode shrinking (Single DEX 7.46 MB, APK 11.89 MB), serta pembuatan paket produksi Android App Bundle (AAB) bertanda tangan valid.
* **iTalase Agentic Dashboard (React 18 & Antigravity Skill):** Dashboard web terintegrasi (`iTalase Agentic Dashboard/`) berbasis React 18, Apple Design Fluid Interface, dan Kanban interaktif dengan drag-and-drop untuk pemantauan telemetri, metrik coverage JaCoCo (>=75%), izin otonom (*Turbo Permissions*), dan status hardware secara real-time. Terhubung dengan Antigravity Customization System via skill global `italase-agentic-dashboard` dan project rules [`AGENTS.md`](file:///c:/Users/anand/StudioProjects/iTalase/AGENTS.md) sehingga agen AI di sesi baru otomatis tersinkronisasi.

---

## Bahasa Desain

Aplikasi memakai satu bahasa visual yang konsisten: **Liquid Glass × Glassmorphism × Material3
Expressive** — permukaan kaca berlapis dengan kedalaman (specular sheen + shadow bertint), **tanpa
garis tepi** (`BorderStroke` adalah penanda paling kentara kartu Material murah).

* **Resep Permukaan Tunggal:** `Modifier.glassSurface()`. Kartu, baris, kolom input,
  chip, tombol, panel, dan dialog semuanya turunan resep itu.
* **Top App Bar Liquid Glass:** Header dinamis yang merespons fraksi kolaps scroll (`scrollBehavior.state.collapsedFraction`),
  meningkatkan elevasi kaca, translusensi, dan specular sheen saat konten digulir ke atas. Pada mode landscape, tinggi header dipadatkan ke 52dp untuk memaksimalkan area kerja kasir.
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
    * Antarmuka kasir cepat: cari (nama/barcode/kategori), saring per kategori, susun keranjang, lalu bayar.
    * Mode Landscape POS: Split layout dengan panel katalog produk di kiri dan panel keranjang belanja independen di kanan.
    * Ticker status keranjang interaktif pada top bar eyebrow: menampilkan jumlah item aktif
      (`X BARANG DI KERANJANG`) saat keranjang terisi, atau beralih ke total inventaris (`X BARANG TERCATAT`) saat kosong.
    * Metode bayar: **Tunai, Transfer, atau Bon** (utang pelanggan).
    * Stok berkurang otomatis dalam **satu transaksi database**, mencegah inkonsistensi data.
    * Penghitungan kembalian otomatis saat nominal diterima dimasukkan.

* **Keandalan Akses Offline & Keamanan Data**
    * Seluruh data tersimpan lokal via Room DB; operasional harian kasir tidak membutuhkan jaringan sama sekali.
    * Database terenkripsi penuh *at-rest* via SQLCipher 4. Kunci dilindungi hardware platform (Android Keystore / iOS Keychain).
    * Migrasi transparan: database versi lama yang belum terenkripsi akan dienkripsi otomatis saat pembaruan pertama tanpa intervensi manual.
    * Skema v4 mengindeks kolom pencarian dan relasi kunci asing (`dateMillis`, `saleId`, `category`, `debtId`, `status`) demi query yang instan.

* **Riwayat & Laporan**
    * Riwayat transaksi dengan saring kategori dan tampilan detail struk.
    * Kemampuan mengedit nama pelanggan, catatan, metode pembayaran, dan nominal uang diterima (`tendered`) pada nota yang tersimpan.
    * **Ekspor Laporan Penjualan ke PDF**: Laporan riwayat transaksi dapat diekspor langsung ke berkas PDF
      dengan filter rentang tanggal dan dibagikan lewat *system share sheet* yang terlindungi.
    * Ringkasan harian di Beranda: omzet, laba, jumlah transaksi, pengeluaran, dan pendapatan bersih.

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

---

## Status Fitur

### Sudah Selesai (Production Ready)
* Seluruh fitur bisnis, inventaris, kasir, bon, pengeluaran, dan ekspor laporan PDF.
* Enkripsi database SQLCipher 4 dengan manajemen kunci platform hardware.
* Dukungan penuh orientasi layar Landscape dan Portrait dengan antarmuka POS Split Pane.
* Bahasa desain Liquid Glass × Glassmorphism × Material3 Expressive di seluruh kartu, tombol, dialog, dan FAB.
* Top app bar reaktif scroll dengan efek liquid glass docking dan ticker angka directional terisolasi di semua layar.
* Integrasi visual `@Preview` Compose Multiplatform menggunakan Android CLI sidecar.
* Konfigurasi penandatanganan rilis (*release signing*) aman via `local.properties`.
* Minifikasi dan optimasi bytecode R8/ProGuard pada build release Android (11.89 MB, Single DEX).
* Pembuatan Android App Bundle (AAB) produksi yang terverifikasi (`androidApp-release.aab`).
* Pengujian unit komprehensif: **77 Unit Tests (100% lulus)** dengan cakupan instruksi JaCoCo **76.18%** (memenuhi gate $\ge 75\%$).
* Pengujian instrumentasi pada hardware fisik **Samsung Galaxy A54 5G**: 5 pengujian lolos (lifecycle, rotasi landscape-portrait cycle, dan verifikasi enkripsi binary SQLite).
* Instalasi sukses build release signed v1.0.0 (100000) ke perangkat fisik via USB ADB.
* Pembuatan **iTalase Agentic Dashboard (React.js Edition)** mandiri dengan kemampuan Drag & Drop Kanban, modal interaktif pembuatan task, live telemetri poller, dan integrasi penuh dengan Antigravity Customization via skill `italase-agentic-dashboard` serta aturan proyek `AGENTS.md` / `GEMINI.md`.

### Rencana Pengembangan Berikutnya (Roadmap)
* **Kesiapan Distribusi Google Play Store:**
    * Penyiapan Google Play Console Closed Testing track (tahap pengujian tertutup 14 hari dengan minimal 20 tester).
    * Pembuatan halaman kebijakan privasi publik (*Privacy Policy*) yang menegaskan model *zero tracking* & *offline-first*.
    * Pembuatan aset visual Play Store: App Icon (512×512 px), Feature Graphic (1024×500 px), dan tangkapan layar perangkat beresolusi tinggi.
* **Pemindaian Barcode via Kamera:** Integrasi CameraX / Google Code Scanner (ML Kit) untuk membaca barcode fisik tanpa perangkat scanner eksternal.
* **Diskon & Promosi:** Dukungan diskon persentase maupun nominal potongan harga tetap per item atau total keranjang.
* **Pembatalan / Void Transaksi:** Fitur pembatalan transaksi yang sudah tersimpan dengan audit log alasan pembatalan dan pengembalian stok otomatis.
* **Pencatatan Pembelian / Kulakan:** Pencatatan transaksi pembelian barang dari pemasok (*purchase orders*) untuk otomatisasi penambahan stok inventaris.

---

## Prinsip Pengembangan

* **Satu sumber kebenaran.** Aturan yang berlaku lintas layar diletakkan di satu tempat dan diuji — bukan disalin ke tiap layar.
* **Satu bahasa visual.** Komponen baru harus memakai primitif kaca yang sudah ada (`Modifier.glassSurface()`).
* **Kerjakan hanya yang berubah.** Data yang tidak berubah saat pengguna mengetik tidak dihitung ulang.
* **Ukur, jangan mengklaim.** Optimasi, performa frame rate, dan perbaikan kontras disertai angka, tes, dan build artifact yang dapat diverifikasi ulang.
