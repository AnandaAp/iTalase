package com.anlian.italase.shared.ui.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Menegakkan kontrak back: hanya lima tab root yang memicu konfirmasi keluar.
// Sub-screen (riwayat, detail struk, pengeluaran) harus tetap bisa di-pop.
class TabRoutesTest {

    @Test
    fun allFiveBottomTabsAreTabRoutes() {
        listOf(
            Routes.HOME,
            Routes.CASHIER,
            Routes.STOCK,
            Routes.DEBT,
            Routes.MORE,
        ).forEach { assertTrue(Routes.isTabRoute(it), "$it harus jadi tab root") }
    }

    @Test
    fun pushedScreensAreNotTabRoutes() {
        listOf(
            Routes.HISTORY,
            Routes.HISTORY_DETAIL,
            Routes.EXPENSE,
        ).forEach { assertFalse(Routes.isTabRoute(it), "$it harus bisa di-pop dengan back") }
    }

    @Test
    fun nullRouteIsNotTabRoute() {
        // Sebelum back stack terisi, back tidak boleh langsung memicu dialog keluar.
        assertFalse(Routes.isTabRoute(null))
    }

    @Test
    fun unknownRouteIsNotTabRoute() {
        assertFalse(Routes.isTabRoute("tidak-ada"))
    }

    /**
     * Daftar tab dipakai sebagai himpunan, bukan list terurut: duplikat membuat tombol back terasa
     * acak karena satu rute dihitung dua kali.
     */
    @Test
    fun tabRouteSetHasNoDuplicates() {
        assertTrue(Routes.TAB_ROUTES.size == 5, "tab root tidak boleh duplikat: ${Routes.TAB_ROUTES}")
    }

    // ── Tombol back di top bar: HANYA Riwayat + Pengeluaran ──
    /**
     * Jumlah rute ber-tombol back dikunci ke 2 supaya layar baru tidak diam-diam mendapat tombol
     * back di top bar saat ditambahkan ke daftar.
     */
    @Test
    fun onlyHistoryAndExpenseGetABackButton() {
        assertTrue(Routes.hasBackButton(Routes.HISTORY))
        assertTrue(Routes.hasBackButton(Routes.EXPENSE))
        assertTrue(Routes.BACK_ROUTES.size == 2, "jangan tambah tanpa sengaja: ${Routes.BACK_ROUTES}")
    }

    @Test
    fun tabsAndDetailScreenHaveNoBackButton() {
        // Tab root tidak butuh (ada bottom nav); Detail Struk merender tombolnya
        // sendiri, jadi kalau ia ikut dapat, tombolnya jadi dua.
        (Routes.TAB_ROUTES + Routes.HISTORY_DETAIL + "tidak-ada").forEach {
            assertFalse(Routes.hasBackButton(it), "$it tidak boleh punya tombol back di top bar")
        }
        assertFalse(Routes.hasBackButton(null))
    }
}