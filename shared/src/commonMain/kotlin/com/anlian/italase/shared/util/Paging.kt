package com.anlian.italase.shared.util

/** Ukuran satu halaman daftar. Setiap muat-tambahan tepat sebanyak ini. */
const val PAGE_SIZE = 20

/**
 * Apakah baris terakhir sudah (hampir) terlihat sehingga halaman berikutnya
 * perlu dimuat? Fungsi murni supaya bisa diuji tanpa Compose.
 *
 * [buffer] = berapa baris sebelum ujung yang sudah dianggap "sampai bawah".
 * Tanpa buffer, user sempat melihat daftar berhenti sebelum item berikutnya
 * muncul; dengan buffer, halaman berikutnya sudah jalan saat masih menggulir.
 */
fun endOfListReached(lastVisibleIndex: Int, totalCount: Int, buffer: Int): Boolean =
    totalCount > 0 && lastVisibleIndex >= totalCount - 1 - buffer

/**
 * Apakah masih ada halaman berikutnya?
 *
 * Paging di sini memakai LIMIT yang BERTUMBUH: permintaan ke-n meminta
 * `n * PAGE_SIZE` baris teratas, bukan OFFSET. Sebuah permintaan yang meminta n
 * baris tapi hanya menerima kurang dari n berarti tabelnya sudah habis — jadi
 * tidak ada yang perlu dimuat lagi. Tidak perlu query COUNT terpisah.
 *
 * Kenapa LIMIT bertumbuh, bukan LIMIT + OFFSET: OFFSET memutus reaktivitas
 * Room (tiap halaman jadi query terpisah yang harus disatukan sendiri di state),
 * sementara LIMIT bertumbuh tetap satu query reaktif — transaksi baru langsung
 * muncul di halaman pertama tanpa kode tambahan. Ongkosnya query ke-n membaca
 * ulang n*20 baris; pada SQLite lokal dengan ratusan baris itu tidak terukur.
 */
fun hasMorePages(loadedCount: Int, requestedLimit: Int): Boolean = loadedCount >= requestedLimit
