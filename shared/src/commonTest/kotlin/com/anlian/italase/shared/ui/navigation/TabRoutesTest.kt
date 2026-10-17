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

    @Test
    fun tabRouteSetHasNoDuplicates() {
        assertTrue(Routes.TAB_ROUTES.size == 5, "tab root tidak boleh duplikat: ${Routes.TAB_ROUTES}")
    }
}