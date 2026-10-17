package com.anlian.italase.shared.ui.navigation

object Routes {
    const val HOME = "home"
    const val CASHIER = "kasir"
    const val STOCK = "stok"
    const val HISTORY = "riwayat"
    const val HISTORY_DETAIL = "riwayat_detail"
    const val DEBT = "bon"
    const val EXPENSE = "biaya"
    const val MORE = "more"

    // Lima root tab. Hanya route di sini yang boleh "root" back stack:
    // tekan back di tab manapun = konfirmasi keluar, bukan kembali ke tab lain.
    val TAB_ROUTES: Set<String> = setOf(HOME, CASHIER, STOCK, DEBT, MORE)

    fun isTabRoute(route: String?): Boolean = route != null && route in TAB_ROUTES
}
