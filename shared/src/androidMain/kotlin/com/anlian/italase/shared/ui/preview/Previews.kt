package com.anlian.italase.shared.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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

@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    ITalaseTheme {
        WarungBackground {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    PreviewWrapper {
        DashboardContent(
            summary = sampleSummary(),
            top = sampleTop(),
            piutang = 48000,
            lowStock = sampleProducts().filter { it.stock <= 3 },
            activeDebtCount = 1,
            error = "",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardEmptyPreview() {
    PreviewWrapper {
        DashboardContent(
            summary = DaySummary(),
            top = emptyList(),
            piutang = 0,
            lowStock = emptyList(),
            activeDebtCount = 0,
            error = "",
        )
    }
}

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

@Preview(showBackground = true)
@Composable
fun PayDialogPreview() {
    PreviewWrapper {
        PayDialog(total = 45500, onDismiss = {}, onConfirm = { _, _, _ -> })
    }
}

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

@Preview(showBackground = true)
@Composable
fun ProductFormPreview() {
    PreviewWrapper {
        ProductFormDialog(initial = sampleProducts()[1], products = sampleProducts(), onDismiss = {}, onSave = { _, _, _, _, _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryPreview() {
    PreviewWrapper {
        HistoryContent(sales = sampleSales())
    }
}

@Preview(showBackground = true)
@Composable
fun SaleDetailPreview() {
    PreviewWrapper {
        SaleDetailScreen(detail = sampleSaleDetail(), message = "", showEditDialog = false)
    }
}

@Preview(showBackground = true)
@Composable
fun SaleEditPreview() {
    PreviewWrapper {
        SaleEditDialog(initialCustomer = "Bu RT", initialNote = "", onDismiss = {}, onSave = { _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun DebtPreview() {
    PreviewWrapper {
        DebtContent(debts = sampleDebts(), message = "")
    }
}

@Preview(showBackground = true)
@Composable
fun PayDebtPreview() {
    PreviewWrapper {
        PayDebtDialog(onDismiss = {}, onSave = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ExpensePreview() {
    PreviewWrapper {
        ExpenseContent(items = sampleExpenses(), message = "")
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseFormPreview() {
    PreviewWrapper {
        ExpenseFormDialog(onDismiss = {}, onSave = { _, _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun MorePreview() {
    PreviewWrapper {
        MoreScreen()
    }
}
