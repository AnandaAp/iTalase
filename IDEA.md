**iTalase** adalah aplikasi manajemen stok barang dan kasir (*Point of Sale*) modern berbasis *mobile* yang dirancang untuk performa tinggi, efisiensi operasional, dan keandalan transaksi secara *offline-first*.

**Arsitektur & Teknologi Utama**

* **Kotlin Multiplatform (KMP):** Berbagi *business logic*, data layer, dan manajemen status (*state management*) secara penuh antara platform iOS dan Android, memastikan konsistensi fitur dan efisiensi pemeliharaan kode.
* **Compose Multiplatform:** Menyediakan antarmuka pengguna (UI) yang deklaratif, responsif, dan *native* di kedua platform (Android & iOS) dengan basis kode UI yang seragam.
* **Room KMP (Offline-First):** Memanfaatkan Room Database versi multiplatform untuk penyimpanan data lokal secara permanen. Aplikasi tetap dapat beroperasi penuh tanpa koneksi internet (input barang, pencatatan transaksi, dan cek stok).
* **Latest Dependencies:** Dibangun menggunakan versi pustaka (*library*) dan dependensi Kotlin/AndroidX paling mutakhir untuk menjamin stabilitas, keamanan, serta pemanfaatan fitur performa terbaru.

---

**Fitur Utama**

* **Manajemen Stok & Inventaris**
* Pencatatan produk lengkap (nama, SKU/barcode, kategori, harga beli/jual, dan jumlah stok).
* Pembaruan stok secara otomatis setiap kali terjadi transaksi penjualan atau pembelian.
* Peringatan stok menipis (*low stock alert*) untuk mencegah kehabisan barang.


* **Sistem Kasir (Point of Sale)**
* Antarmuka kasir cepat dan intuitif untuk pemrosesan transaksi harian.
* Dukungan pemindaian *barcode* produk menggunakan kamera perangkat.
* Kalkulasi otomatis total belanja, diskon, dan kembalian.


* **Keandalan Akses Offline**
* Seluruh data barang, riwayat transaksi, dan laporan tersimpan secara lokal di dalam perangkat via Room DB.
* Tidak bergantung pada *server* cloud untuk operasional harian kasir.


* **Riwayat & Laporan Penjualan**
* Pencatatan riwayat transaksi secara terperinci.
* Ringkasan pendapatan dan laporan penjualan harian maupun bulanan untuk analisis bisnis dasar.
