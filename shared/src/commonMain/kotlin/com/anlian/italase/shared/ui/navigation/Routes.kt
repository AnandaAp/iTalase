package com.anlian.italase.shared.ui.navigation

/**
 * Nama rute NavHost — satu-satunya tempat string rute boleh ditulis.
 * Layar tidak menyebut rute sebagai literal; mereka menerima callback dari
 * [AppNavHost], jadi mengganti nama rute tidak perlu menyisir layar.
 */
object Routes {
    /** Beranda (tab root). */
    const val HOME = "home"

    /** Kasir (tab root). */
    const val CASHIER = "kasir"

    /** Stok (tab root). */
    const val STOCK = "stok"

    /** Riwayat penjualan (sub-screen, punya tombol back). */
    const val HISTORY = "riwayat"

    /** Detail satu struk (sub-screen; tombol back-nya dirender layarnya sendiri). */
    const val HISTORY_DETAIL = "riwayat_detail"

    /** Bon / piutang (tab root). */
    const val DEBT = "bon"

    /** Pengeluaran (sub-screen, punya tombol back). */
    const val EXPENSE = "biaya"

    /** Menu Lainnya (tab root). */
    const val MORE = "more"

    /** Pengaturan Toko (sub-screen, punya tombol back). */
    const val SETTINGS = "pengaturan"

    /** Pengaturan Pembayaran & QRIS (sub-screen, punya tombol back). */
    const val SETTINGS_PAYMENT = "pengaturan_pembayaran"

    /** Setup QRIS: ambil foto / pilih galeri (sub-screen, punya tombol back). */
    const val QRIS_SETUP = "qris_setup"

    /** Review & konfirmasi QRIS (sub-screen, punya tombol back). */
    const val QRIS_REVIEW = "qris_review"

    // Lima root tab. Hanya route di sini yang boleh "root" back stack:
    // tekan back di tab manapun = konfirmasi keluar, bukan kembali ke tab lain.
    val TAB_ROUTES: Set<String> = setOf(HOME, CASHIER, STOCK, DEBT, MORE)

    /** True kalau rute ini root tab (lihat [TAB_ROUTES]); null = belum ada destinasi. */
    fun isTabRoute(route: String?): Boolean = route != null && route in TAB_ROUTES

    // Layar sub-screen yang punya tombol back di top bar.
    //  • Tab root tidak perlu — ada bottom nav.
    //  • Detail Struk tidak perlu — ia merender tombolnya sendiri.
    // Ditaruh di sini, bukan di composable, supaya aturannya satu tempat dan
    // bisa diuji; kalau disebar, cepat atau lambat ada layar yang dapat tombol
    // tanpa sengaja atau kehilangan tombolnya saat di-refactor.
    val BACK_ROUTES: Set<String> = setOf(
        HISTORY,
        EXPENSE,
        SETTINGS,
        SETTINGS_PAYMENT,
        QRIS_SETUP,
        QRIS_REVIEW,
    )

    /** True kalau top bar harus merender tombol back untuk rute ini. */
    fun hasBackButton(route: String?): Boolean = route != null && route in BACK_ROUTES
}
