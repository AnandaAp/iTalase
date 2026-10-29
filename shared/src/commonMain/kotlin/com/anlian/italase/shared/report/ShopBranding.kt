package com.anlian.italase.shared.report

import androidx.compose.runtime.Immutable

/**
 * Token identitas & branding warung untuk cetak struk (thermal Bluetooth)
 * dan ekspor laporan PDF (AUD-104 / CARD-104).
 */
@Immutable
data class ShopBranding(
    /** Nama toko / warung, misal "Warung Berkah Bu Siti" atau [REPORT_APP_NAME]. */
    val shopName: String = REPORT_APP_NAME,
    /** Tagline atau slogan toko, misal "Kasir & stok warung, offline" atau [REPORT_TAGLINE]. */
    val tagline: String = REPORT_TAGLINE,
    /** Alamat toko / lokasi fisik warung, misal "Jl. Raya Pasar Minggu No. 42". */
    val address: String = "",
    /** Nomor telepon / WhatsApp atau kontak kasir, misal "0812-3456-7890". */
    val phone: String = "",
    /** Catatan kaki / ucapan terima kasih di struk kasir. */
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!",
    /** Catatan kaki sumber data laporan PDF. */
    val reportFooter: String = REPORT_FOOTER,
)
