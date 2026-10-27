package com.anlian.italase.shared.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.anlian.italase.shared.data.local.entity.PayMethod
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
import com.anlian.italase.shared.ui.screens.ProductContent
import com.anlian.italase.shared.ui.screens.ProductFormDialog
import com.anlian.italase.shared.ui.screens.SaleDetailScreen
import com.anlian.italase.shared.ui.screens.SaleEditDialog
import com.anlian.italase.shared.ui.theme.ITalaseTheme
import com.anlian.italase.shared.ui.theme.WarungBackground
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

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
