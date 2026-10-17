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

    // HANYA dua layar ini yang punya tombol back di top bar.
    //  • Tab root tidak perlu — ada bottom nav.
    //  • Detail Struk tidak perlu — ia merender tombolnya sendiri.
    // Ditaruh di sini, bukan di composable, supaya aturannya satu tempat dan
    // bisa diuji; kalau disebar, cepat atau lambat ada layar yang dapat tombol
    // tanpa sengaja atau kehilangan tombolnya saat di-refactor.
    val BACK_ROUTES: Set<String> = setOf(HISTORY, EXPENSE)

    fun hasBackButton(route: String?): Boolean = route != null && route in BACK_ROUTES
}
