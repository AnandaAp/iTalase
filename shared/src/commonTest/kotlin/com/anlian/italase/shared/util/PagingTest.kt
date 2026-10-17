package com.anlian.italase.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mengunci aritmetika paging murni, tanpa Compose maupun Room.
 *
 * Bug di sini tidak melempar exception — gejalanya cuma "daftar berhenti setengah", jadi
 * perilakunya perlu dikunci eksplisit.
 */
class PagingTest {

    @Test
    fun emptyListNeverTriggersLoadMore() {
        assertFalse(endOfListReached(lastVisibleIndex = -1, totalCount = 0, buffer = 3))
    }

    @Test
    fun listShorterThanScreenTriggersImmediately() {
        // 5 baris di layar yang muat ~8 baris: SEMUANYA terlihat, jadi baris
        // terakhir ada di indeks 4 dan halaman berikutnya harus jalan tanpa
        // menunggu user menggulir (memang tidak ada yang bisa digulir).
        assertTrue(endOfListReached(lastVisibleIndex = 4, totalCount = 5, buffer = 3))

        // Tapi kalau baru baris pertama yang terlihat, artinya masih ada yang
        // bisa digulir → jangan memuat dulu.
        assertFalse(endOfListReached(lastVisibleIndex = 0, totalCount = 5, buffer = 3))
    }

    @Test
    fun bufferTriggersBeforeTheVeryLastRow() {
        // 30 baris, buffer 3 → mulai memuat saat baris indeks 26 terlihat.
        assertFalse(endOfListReached(lastVisibleIndex = 25, totalCount = 30, buffer = 3))
        assertTrue(endOfListReached(lastVisibleIndex = 26, totalCount = 30, buffer = 3))
        assertTrue(endOfListReached(lastVisibleIndex = 29, totalCount = 30, buffer = 3))
    }

    /**
     * Halaman penuh berarti mungkin masih ada lanjutannya.
     *
     * Inilah alasan jumlah baris dipakai sebagai penanda, bukan query COUNT terpisah (lihat catatan
     * `hasMorePages`).
     */
    @Test
    fun hasMoreWhileSqlReturnedAFullPage() {
        assertTrue(hasMorePages(loadedCount = 20, requestedLimit = 20))
        assertTrue(hasMorePages(loadedCount = 40, requestedLimit = 40))
    }

    @Test
    fun noMoreWhenSqlReturnedLessThanRequested() {
        assertEquals(false, hasMorePages(loadedCount = 13, requestedLimit = 20))
        assertEquals(false, hasMorePages(loadedCount = 0, requestedLimit = 20))
    }

    /**
     * Nilai 20 bukan angka bebas: "halaman penuh" dipakai sebagai penanda habis/tidak, jadi harus
     * sama dengan pengali limit di ViewModel.
     */
    @Test
    fun pageSizeIsTwenty() {
        assertTrue(PAGE_SIZE == 20, "satu halaman = 20 baris")
    }
}
