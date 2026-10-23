# iTalase — Ide & Arah Produk

**iTalase** adalah aplikasi manajemen stok barang dan kasir (*Point of Sale*)
untuk warung dan toko kecil. Ditujukan untuk **performa tinggi, efisiensi operasional, dan keandalan
transaksi secara *offline-first***: input barang, pencatatan transaksi, dan laporan harian semuanya
berjalan tanpa internet.

**Arsitektur & Teknologi Utama**

* **Kotlin Multiplatform (KMP):** Berbagi *business logic*, data layer, dan manajemen status secara
  penuh antara iOS dan Android — konsistensi fitur dan efisiensi pemeliharaan kode.
* **Compose Multiplatform:** UI deklaratif dan *native* di kedua platform dari satu basis kode.
* **Room KMP + SQLCipher (Offline-First & Terenkripsi):** Seluruh data barang, transaksi, dan laporan
  tersimpan lokal di perangkat dan terenkripsi *at-rest* via SQLCipher 4. Kunci dikelola aman
  lewat platform keystore (Android Keystore dan iOS Keychain), dengan migrasi otomatis dari database
  polos lama tanpa kehilangan data.
* **Generator PDF Mandiri:** Pembuat berkas PDF murni (*zero-dependency*) dalam Kotlin untuk ekspor
  laporan riwayat penjualan tanpa dependensi pustaka luar.
* **Koin + Coroutines/Flow:** Injeksi dependensi dan aliran data reaktif dari database ke layar.
* **Versi pustaka terkini** dipusatkan di `gradle/libs.versions.toml`.

---

**Bahasa Desain**

Aplikasi memakai satu bahasa visual yang konsisten: **Liquid Glass × Glassmorphism × Material3
Expressive** — permukaan kaca berlapis dengan kedalaman (specular sheen + shadow bertint), **tanpa
garis tepi** (`BorderStroke` adalah penanda paling kentara kartu Material murah).

* Satu resep permukaan untuk seluruh app: `Modifier.glassSurface()`. Kartu, baris, kolom input,
  chip, tombol, panel, dan dialog semuanya turunan resep itu.
* Blur hanya di overlay statis (bottom bar, latar dialog). Konten yang bergulir tidak di-blur agar
  frame budget aman di HP mid-range.
* **Huruf:** Noto Sans (4 berat statis). **Ikon:** Material Symbols sebagai vector drawable lokal,
  tanpa dependency font ikon berukuran besar.
* Animasi masuk seragam di semua layar (fade + naik, bertahap per blok).

---

**Fitur**

* **Manajemen Stok & Inventaris**
    * Pencatatan produk lengkap: nama, barcode, kategori, harga beli/jual, jumlah stok, stok
      minimum, dan satuan.
    * Barang yang habis selalu diurutkan ke bawah; tiap kelompok tetap urut abjad sehingga barang
      yang tersedia mudah ditemukan.
    * Peringatan stok menipis (*low stock alert*) di Beranda.

* **Sistem Kasir (Point of Sale)**
    * Antarmuka kasir cepat: cari (nama/barcode/kategori), saring per kategori, susun keranjang,
      lalu bayar.
    * Metode bayar: **Tunai, Transfer, atau Bon** (utang pelanggan).
    * Stok berkurang otomatis dalam **satu transaksi database**, jadi stok tidak bisa setengah
      terpotong.
    * Kembalian dihitung dari uang diterima.

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

* **Aksesibilitas & Navigasi**
    * Lima tab bawah: Beranda, Kasir, Stok, Bon, Lainnya.
    * **Tombol back eksplisit** di Riwayat dan Pengeluaran. Android punya tombol kembali sistem, iOS
      tidak — tanpa tombol ini pengguna iOS terjebak di halaman tersebut.

---

**Status Fitur**

*Sudah jalan:* seluruh poin di atas.

*Belum dikerjakan — rencana, bukan klaim:*

* **Pemindaian barcode lewat kamera.** Produk sudah punya kolom barcode dan pencarian
  mencocokkannya, tetapi belum ada pembaca dari kamera.
* **Diskon.** Total masih murni jumlah baris keranjang.
* **Pembatalan/void transaksi.** Struk yang sudah tersimpan belum bisa dibatalkan dari UI; yang
  tersedia hanya mengubah nama pelanggan dan catatan.
* **Dashboard agregasi bulanan.** Tampilan Beranda saat ini berfokus pada ringkasan harian; untuk
  laporan bulanan/rentang fleksibel saat ini diakomodasi melalui fitur ekspor PDF di Riwayat.
* **Pembelian/kulakan sebagai transaksi.** Penambahan stok dilakukan dengan menyunting produk, belum
  sebagai transaksi pembelian yang tercatat.

---

**Prinsip Pengembangan**

* **Satu sumber kebenaran.** Aturan yang berlaku lintas layar (urutan produk, ukuran halaman, layar
  mana yang punya tombol back) diletakkan di satu tempat dan diuji — bukan disalin ke tiap layar.
* **Satu bahasa visual.** Komponen baru harus memakai primitif kaca yang sudah ada, bukan membuat
  gaya kartu/dialog ketiga.
* **Kerjakan hanya yang berubah.** Data yang tidak berubah saat pengguna mengetik tidak dihitung
  ulang saat pengguna mengetik.
* **Ukur, jangan mengklaim.** Optimasi dan perbaikan kontras disertai angka dan tes yang bisa
  dijalankan ulang.
