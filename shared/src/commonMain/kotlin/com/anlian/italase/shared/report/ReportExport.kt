package com.anlian.italase.shared.report

/**
 * Ekspor laporan PDF, titik masuk yang dipakai UI.
 *
 * Berkas disimpan lewat [savePdf] (implementasi per platform) sehingga pemanggil
 * tidak perlu tahu soal `FileProvider` Android atau `UIActivityViewController` iOS.
 * Alurnya: baca logo → bentuk [SalesReport] → render PDF → simpan/bagikan.
 *
 * Melempar [Throwable] kalau penyimpanan gagal; pemanggil menampilkan pesan.
 * Sengaja tidak menelan error: laporan yang "gagal tanpa kabar" lebih buruk
 * daripada pesan gagal yang jelas.
 */
expect suspend fun savePdf(suggestedName: String, bytes: ByteArray): Boolean

/** Nama aplikasi di kop laporan. */
const val REPORT_APP_NAME = "iTalase"

/** Kalimat kecil di bawah nama aplikasi di kop laporan. */
const val REPORT_TAGLINE = "Kasir & stok warung, offline"

/** Sumber data yang ditulis di kaki setiap halaman. */
const val REPORT_FOOTER = "Sumber data: database lokal iTalase"

/**
 * Batas baris untuk satu ekspor PDF.
 *
 * Bukan angka ajaib "cukup banyak": 5.000 baris ≈ 60 halaman A4, jauh di atas
 * kebiasaan satu laporan warung, sementara laporan tanpa batas berarti membangun
 * dokumen raksasa di memori HP kelas bawah. Kalau batasnya tercapai, laporan
 * menulis catatan di blok total alih-alih diam-diam terpotong.
 */
const val EXPORT_LIMIT = 5_000
