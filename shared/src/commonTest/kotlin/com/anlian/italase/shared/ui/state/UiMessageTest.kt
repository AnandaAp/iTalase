package com.anlian.italase.shared.ui.state

import com.anlian.italase.shared.data.repository.ErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UiMessageTest {

    @Test
    fun testErrorCodeToMessageMapping() {
        assertEquals(UiMessage.CartEmpty, ErrorCode.EMPTY_CART.toMessage())
        assertEquals(UiMessage.ProductGone("Kopi"), ErrorCode.PRODUCT_GONE.toMessage("Kopi"))
        assertEquals(UiMessage.QtyInvalid, ErrorCode.QTY_INVALID.toMessage())
        assertEquals(UiMessage.StockShort("Beras"), ErrorCode.STOCK_SHORT.toMessage("Beras"))
        assertEquals(UiMessage.CustomerRequired, ErrorCode.CUSTOMER_REQUIRED.toMessage())
        assertEquals(UiMessage.ExpenseNameRequired, ErrorCode.EXPENSE_NAME_REQUIRED.toMessage())
        assertEquals(UiMessage.AmountPositive, ErrorCode.AMOUNT_POSITIVE.toMessage())
        assertEquals(UiMessage.DebtMissing, ErrorCode.DEBT_MISSING.toMessage())
        assertEquals(UiMessage.DebtAlreadyPaid, ErrorCode.ALREADY_PAID.toMessage())
    }

    @Test
    fun testUiMessageVariants() {
        val stockShort = UiMessage.StockShort("Susu")
        assertEquals("Susu", stockShort.name)

        val txnSaved = UiMessage.TxnSaved(123L)
        assertEquals(123L, txnSaved.id)

        val productGone = UiMessage.ProductGone("Gula")
        assertEquals("Gula", productGone.name)

        val debtPaid = UiMessage.DebtPaid(50000L)
        assertEquals(50000L, debtPaid.amount)

        // Verify objects are non-null and distinct
        val objects = listOf(
            UiMessage.ProductSaved,
            UiMessage.ProductDeleted,
            UiMessage.SaveFailed,
            UiMessage.DeleteFailed,
            UiMessage.NameRequired,
            UiMessage.PricePositive,
            UiMessage.CheckoutFailed,
            UiMessage.CartEmpty,
            UiMessage.QtyInvalid,
            UiMessage.SummaryFailed,
            UiMessage.DebtSaved,
            UiMessage.DebtAlreadyPaid,
            UiMessage.DebtSaveFailed,
            UiMessage.DebtPayFailed,
            UiMessage.CustomerRequired,
            UiMessage.ExpenseNameRequired,
            UiMessage.AmountPositive,
            UiMessage.DebtMissing,
            UiMessage.ExpenseSaved,
            UiMessage.ExpenseSaveFailed,
            UiMessage.SaleInfoSaved,
            UiMessage.GenericFailure,
            UiMessage.ReportExported,
            UiMessage.ReportExportFailed
        )
        assertEquals(24, objects.distinct().size)
    }
}
