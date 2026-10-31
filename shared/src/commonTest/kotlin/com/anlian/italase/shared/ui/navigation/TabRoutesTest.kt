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
            Routes.SETTINGS,
            Routes.SETTINGS_PAYMENT,
            Routes.QRIS_SETUP,
            Routes.QRIS_REVIEW,
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

    // ── Tombol back di top bar: Riwayat, Pengeluaran, Pengaturan, Pembayaran, QRIS Setup & Review ──
    /**
     * Jumlah rute ber-tombol back dikunci ke 6 supaya layar baru tidak diam-diam mendapat tombol
     * back di top bar saat ditambahkan ke daftar.
     */
    @Test
    fun onlyHistoryAndExpenseGetABackButton() {
        assertTrue(Routes.hasBackButton(Routes.HISTORY))
        assertTrue(Routes.hasBackButton(Routes.EXPENSE))
        assertTrue(Routes.hasBackButton(Routes.SETTINGS))
        assertTrue(Routes.hasBackButton(Routes.SETTINGS_PAYMENT))
        assertTrue(Routes.hasBackButton(Routes.QRIS_SETUP))
        assertTrue(Routes.hasBackButton(Routes.QRIS_REVIEW))
        assertTrue(Routes.BACK_ROUTES.size == 6, "jangan tambah tanpa sengaja: ${Routes.BACK_ROUTES}")
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

    @Test
    fun fullPageScrollableRoutesIncludeHomeMoreAndSettingsSuite() {
        listOf(
            Routes.HOME,
            Routes.MORE,
            Routes.SETTINGS,
            Routes.SETTINGS_PAYMENT,
            Routes.QRIS_SETUP,
            Routes.QRIS_REVIEW,
        ).forEach {
            assertTrue(Routes.isFullPageScrollable(it), "$it harus full page scroll")
        }

        listOf(
            Routes.CASHIER,
            Routes.STOCK,
            Routes.DEBT,
            Routes.HISTORY,
            Routes.HISTORY_DETAIL,
            Routes.EXPENSE,
            null,
            "tidak-ada",
        ).forEach {
            assertFalse(Routes.isFullPageScrollable(it), "$it bukan full page scroll")
        }

        assertTrue(Routes.FULL_PAGE_SCROLL_ROUTES.size == 6)
    }
}