package com.anlian.italase.shared.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.data.repository.DaySummary
import com.anlian.italase.shared.ui.screens.CashierContent
import com.anlian.italase.shared.ui.screens.DashboardContent
import com.anlian.italase.shared.ui.screens.DebtContent
import com.anlian.italase.shared.ui.screens.ExpenseContent
import com.anlian.italase.shared.ui.screens.ExpenseFormDialog
import com.anlian.italase.shared.ui.screens.HistoryContent
import com.anlian.italase.shared.ui.screens.MoreScreen
import com.anlian.italase.shared.ui.screens.PayDebtDialog
import com.anlian.italase.shared.ui.screens.PayDialog
import com.anlian.italase.shared.ui.screens.PaymentSettingsScreen
import com.anlian.italase.shared.ui.screens.ProductContent
import com.anlian.italase.shared.ui.screens.ProductFormDialog
import com.anlian.italase.shared.ui.screens.QrisPromptBanner
import com.anlian.italase.shared.ui.screens.QrisReviewContent
import com.anlian.italase.shared.ui.screens.QrisSetupContent
import com.anlian.italase.shared.ui.screens.SaleDetailScreen
import com.anlian.italase.shared.ui.screens.SaleEditDialog
import com.anlian.italase.shared.ui.theme.ITalaseTheme
import com.anlian.italase.shared.ui.theme.WarungBackground
import java.io.ByteArrayOutputStream
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Pembungkus wajib setiap preview: tema + latar warung.
 *
 * Tanpa ini preview menampilkan warna & latar default yang tidak pernah dipakai aplikasi,
 * jadi panel preview berhenti berguna sebagai pemeriksaan cepat.
 */
@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    ITalaseTheme {
        WarungBackground {
            content()
        }
    }
}

/**
 * Dashboard terisi: omzet, produk terlaris, dan satu piutang aktif.
 */
@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    PreviewWrapper {
        DashboardContent(
            summary = sampleSummary(),
            top = sampleTop().toPersistentList(),
            piutang = 48000,
            lowStock = sampleProducts().filter { it.stock <= 3 }.toPersistentList(),
            activeDebtCount = 1,
            error = "",
        )
    }
}

/**
 * Dashboard kosong — kasus paling gampang salah render (belum ada penjualan hari ini).
 */
@Preview(showBackground = true)
@Composable
fun DashboardEmptyPreview() {
    PreviewWrapper {
        DashboardContent(
            summary = DaySummary(),
            top = persistentListOf(),
            piutang = 0,
            lowStock = persistentListOf(),
            activeDebtCount = 0,
            error = "",
        )
    }
}

/**
 * Kasir dengan dua baris keranjang terisi.
 */
@Preview(showBackground = true)
@Composable
fun CashierPreview() {
    val products = sampleProducts()
    PreviewWrapper {
        CashierContent(
            query = "",
            onQuery = {},
            categories = listOf("Makanan", "Minuman", "Sembako"),
            activeCategory = null,
            onCategory = {},
            list = products,
            cart = mapOf(1L to 2, 3L to 1),
            lines = listOf(
                CartLine(products[0], 2),
                CartLine(products[2], 1),
            ),
            total = 28000,
            message = "",
        )
    }
}

/**
 * Dialog pembayaran dengan total bukan angka bulat, supaya kolom kembalian ikut terpakai.
 */
@Preview(showBackground = true)
@Composable
fun PayDialogPreview() {
    PreviewWrapper {
        PayDialog(total = 45500, onDismiss = {}, onConfirm = { _, _, _ -> })
    }
}

/**
 * Daftar stok terisi, termasuk satu produk habis.
 */
@Preview(showBackground = true)
@Composable
fun ProductPreview() {
    PreviewWrapper {
        ProductContent(
            products = sampleProducts(),
            query = "",
            onQuery = {},
            categories = listOf("Makanan", "Minuman", "Sembako"),
            activeCategory = null,
            onCategory = {},
            message = "",
        )
    }
}

/**
 * Form edit produk tanpa barcode (produk kedua di data contoh).
 */
@Preview(showBackground = true)
@Composable
fun ProductFormPreview() {
    PreviewWrapper {
        ProductFormDialog(initial = sampleProducts()[1], products = sampleProducts(), onDismiss = {}, onSave = { _, _, _, _, _, _, _ -> })
    }
}

/**
 * Riwayat tiga transaksi, termasuk satu BON separuh bayar.
 */
@Preview(showBackground = true)
@Composable
fun HistoryPreview() {
    PreviewWrapper {
        HistoryContent(sales = sampleSales())
    }
}

/**
 * Detail struk tiga item, tanpa dialog edit.
 */
@Preview(showBackground = true)
@Composable
fun SaleDetailPreview() {
    PreviewWrapper {
        SaleDetailScreen(detail = sampleSaleDetail(), message = "", showEditDialog = false)
    }
}

/**
 * Dialog edit info struk dengan nama pelanggan terisi.
 */
@Preview(showBackground = true)
@Composable
fun SaleEditPreview() {
    PreviewWrapper {
        SaleEditDialog(
            initialCustomer = "Bu RT",
            initialNote = "",
            initialPayMethod = PayMethod.TUNAI,
            initialTendered = 50000L,
            total = 45000L,
            onDismiss = {},
            onSave = { _, _, _, _ -> },
        )
    }
}

/**
 * Daftar piutang: satu aktif, satu lunas.
 */
@Preview(showBackground = true)
@Composable
fun DebtPreview() {
    PreviewWrapper {
        DebtContent(debts = sampleDebts(), message = "")
    }
}

/**
 * Dialog pelunasan bon.
 */
@Preview(showBackground = true)
@Composable
fun PayDebtPreview() {
    PreviewWrapper {
        PayDebtDialog(onDismiss = {}, onSave = {})
    }
}

/**
 * Daftar pengeluaran lintas kategori.
 */
@Preview(showBackground = true)
@Composable
fun ExpensePreview() {
    PreviewWrapper {
        ExpenseContent(items = sampleExpenses(), message = "")
    }
}

/**
 * Form pengeluaran kosong.
 */
@Preview(showBackground = true)
@Composable
fun ExpenseFormPreview() {
    PreviewWrapper {
        ExpenseFormDialog(onDismiss = {}, onSave = { _, _, _, _ -> })
    }
}

/**
 * Tab Lainnya apa adanya (tanpa state).
 */
@Preview(showBackground = true)
@Composable
fun MorePreview() {
    PreviewWrapper {
        MoreScreen()
    }
}

/**
 * Pratinjau Kasir dalam orientasi landscape (840 x 390 dp).
 */
@Preview(name = "Cashier Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun CashierLandscapePreview() {
    val products = sampleProducts()
    PreviewWrapper {
        CashierContent(
            query = "",
            onQuery = {},
            categories = listOf("Makanan", "Minuman", "Sembako"),
            activeCategory = null,
            onCategory = {},
            list = products,
            cart = mapOf(1L to 2, 3L to 1),
            lines = listOf(
                CartLine(products[0], 2),
                CartLine(products[2], 1),
            ),
            total = 28000,
            message = "",
        )
    }
}

/**
 * Pratinjau Dashboard dalam orientasi landscape (840 x 390 dp).
 */
@Preview(name = "Dashboard Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun DashboardLandscapePreview() {
    PreviewWrapper {
        DashboardContent(
            summary = sampleSummary(),
            top = sampleTop().toPersistentList(),
            piutang = 48000,
            lowStock = sampleProducts().filter { it.stock <= 3 }.toPersistentList(),
            activeDebtCount = 1,
            error = "",
        )
    }
}

/**
 * Pratinjau Dialog Pembayaran dalam orientasi landscape (840 x 390 dp).
 */
@Preview(name = "PayDialog Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun PayDialogLandscapePreview() {
    PreviewWrapper {
        PayDialog(total = 45500, onDismiss = {}, onConfirm = { _, _, _ -> })
    }
}

/**
 * Pratinjau Dashboard saat memuat data DB (Shimmering Liquid Glass).
 */
@Preview(name = "Dashboard Shimmer Portrait", showBackground = true)
@Composable
fun DashboardShimmerPreview() {
    PreviewWrapper {
        DashboardContent(
            summary = DaySummary(),
            top = persistentListOf(),
            piutang = 0,
            lowStock = persistentListOf(),
            activeDebtCount = 0,
            error = "",
            isLoading = true,
        )
    }
}

/**
 * Pratinjau Dashboard Landscape saat memuat data DB (Shimmering Liquid Glass).
 */
@Preview(name = "Dashboard Shimmer Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun DashboardShimmerLandscapePreview() {
    PreviewWrapper {
        DashboardContent(
            summary = DaySummary(),
            top = persistentListOf(),
            piutang = 0,
            lowStock = persistentListOf(),
            activeDebtCount = 0,
            error = "",
            isLoading = true,
        )
    }
}

/**
 * Pratinjau Kasir saat memuat data awal DB (Shimmering Liquid Glass).
 */
@Preview(name = "Cashier Shimmer Portrait", showBackground = true)
@Composable
fun CashierShimmerPreview() {
    PreviewWrapper {
        CashierContent(
            query = "",
            onQuery = {},
            categories = listOf("Makanan", "Minuman", "Sembako"),
            activeCategory = null,
            onCategory = {},
            list = emptyList(),
            cart = emptyMap(),
            lines = emptyList(),
            total = 0,
            message = "",
            isLoading = true,
        )
    }
}

/**
 * Konfigurasi contoh QRIS untuk `@Preview`.
 */
fun sampleQrisConfig(
    isEnabled: Boolean = true,
    imageBytes: ByteArray? = null,
    promptDismissCount: Int = 0,
) = QrisConfigEntity(
    id = 1,
    isEnabled = isEnabled,
    imageBytes = imageBytes,
    mimeType = "image/jpeg",
    promptDismissCount = promptDismissCount,
    updatedAt = 1_700_000_000_000L,
)

/**
 * Sintesis byte array citra QR code JPEG yang valid dan ringan (160x160) untuk kebutuhan `@Preview`.
 */
fun sampleQrisJpeg(): ByteArray {
    val bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint().apply { color = AndroidColor.BLACK }
    val s = 16f
    for (i in 0 until 10) {
        for (j in 0 until 10) {
            if ((i < 3 && j < 3) || (i > 6 && j < 3) || (i < 3 && j > 6) || (i + j) % 3 == 0) {
                canvas.drawRect(i * s, j * s, (i + 1) * s, (j + 1) * s, paint)
            }
        }
    }
    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    return baos.toByteArray()
}

// ==============================================================================
// 1. QrisPromptBanner Previews
// ==============================================================================

/**
 * Banner prompt penawaran aktivasi QRIS di layar Kasir (Mode Portrait).
 */
@Preview(name = "QrisPromptBanner Portrait", showBackground = true)
@Composable
fun QrisPromptBannerPreview() {
    PreviewWrapper {
        QrisPromptBanner(onNavigateSetup = {}, onDismiss = {})
    }
}

/**
 * Banner prompt penawaran aktivasi QRIS di layar Kasir (Mode Landscape POS Tablet).
 */
@Preview(name = "QrisPromptBanner Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun QrisPromptBannerLandscapePreview() {
    PreviewWrapper {
        QrisPromptBanner(onNavigateSetup = {}, onDismiss = {})
    }
}

// ==============================================================================
// 2. PayDialog with Active QRIS Previews
// ==============================================================================

/**
 * Dialog pembayaran kasir saat tab metode QRIS dipilih dan barcode aktif (Mode Portrait).
 */
@Preview(name = "PayDialog QRIS Active Portrait", showBackground = true)
@Composable
fun PayDialogQrisPreview() {
    val qris = sampleQrisConfig(isEnabled = true, imageBytes = sampleQrisJpeg())
    PreviewWrapper {
        PayDialog(
            total = 45500,
            qrisConfig = qris,
            initialMethod = PayMethod.QRIS,
            onDismiss = {},
            onConfirm = { _, _, _ -> },
        )
    }
}

/**
 * Dialog pembayaran kasir saat tab metode QRIS dipilih dan barcode aktif (Mode Landscape).
 */
@Preview(name = "PayDialog QRIS Active Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun PayDialogQrisLandscapePreview() {
    val qris = sampleQrisConfig(isEnabled = true, imageBytes = sampleQrisJpeg())
    PreviewWrapper {
        PayDialog(
            total = 45500,
            qrisConfig = qris,
            initialMethod = PayMethod.QRIS,
            onDismiss = {},
            onConfirm = { _, _, _ -> },
        )
    }
}

// ==============================================================================
// 3. QrisSetupScreen Previews
// ==============================================================================

/**
 * Layar panduan awal setup barcode QRIS kasir (Mode Portrait).
 */
@Preview(name = "QrisSetup Portrait", showBackground = true)
@Composable
fun QrisSetupScreenPreview() {
    PreviewWrapper {
        QrisSetupContent(onCameraClick = {}, onGalleryClick = {})
    }
}

@Preview(name = "QrisSetup Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun QrisSetupScreenLandscapePreview() {
    PreviewWrapper {
        QrisSetupContent(onCameraClick = {}, onGalleryClick = {})
    }
}

@Composable
fun QrisSetupPreview() = QrisSetupScreenPreview()

// ==============================================================================
// 4. QrisReviewScreen Previews
// ==============================================================================

/**
 * Layar konfirmasi review foto QRIS sebelum disimpan ke DB (Mode Portrait).
 */
@Preview(name = "QrisReview Portrait", showBackground = true)
@Composable
fun QrisReviewScreenPreview() {
    val bitmap = remember { sampleQrisJpeg().decodeToImageBitmap() }
    PreviewWrapper {
        QrisReviewContent(
            imageBitmap = bitmap,
            isSaving = false,
            errorMessage = null,
            onRetake = {},
            onSaveAndActivate = {},
        )
    }
}

/**
 * Layar konfirmasi review foto QRIS sebelum disimpan ke DB (Mode Landscape Split-Pane).
 */
@Preview(name = "QrisReview Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun QrisReviewScreenLandscapePreview() {
    val bitmap = remember { sampleQrisJpeg().decodeToImageBitmap() }
    PreviewWrapper {
        QrisReviewContent(
            imageBitmap = bitmap,
            isSaving = false,
            errorMessage = null,
            onRetake = {},
            onSaveAndActivate = {},
        )
    }
}

@Composable
fun QrisReviewPreview() = QrisReviewScreenPreview()

// ==============================================================================
// 5. PaymentSettingsScreen Previews
// ==============================================================================

/**
 * Layar Pengaturan Metode Pembayaran dengan QRIS aktif terpasang (Mode Portrait).
 */
@Preview(name = "PaymentSettings Active Portrait", showBackground = true)
@Composable
fun PaymentSettingsScreenPreview() {
    val qris = sampleQrisConfig(isEnabled = true, imageBytes = sampleQrisJpeg())
    PreviewWrapper {
        PaymentSettingsScreen(qrisConfig = qris)
    }
}

/**
 * Layar Pengaturan Metode Pembayaran saat belum ada QRIS yang dikonfigurasi (Mode Portrait Empty State).
 */
@Preview(name = "PaymentSettings Empty Portrait", showBackground = true)
@Composable
fun PaymentSettingsEmptyPreview() {
    PreviewWrapper {
        PaymentSettingsScreen(qrisConfig = null)
    }
}

/**
 * Layar Pengaturan Metode Pembayaran dengan QRIS aktif terpasang (Mode Landscape).
 */
@Preview(name = "PaymentSettings Landscape", widthDp = 840, heightDp = 390, showBackground = true)
@Composable
fun PaymentSettingsScreenLandscapePreview() {
    val qris = sampleQrisConfig(isEnabled = true, imageBytes = sampleQrisJpeg())
    PreviewWrapper {
        PaymentSettingsScreen(qrisConfig = qris)
    }
}

@Composable
fun PaymentSettingsPreview() = PaymentSettingsScreenPreview()

