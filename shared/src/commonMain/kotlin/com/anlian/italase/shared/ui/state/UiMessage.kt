package com.anlian.italase.shared.ui.state

import androidx.compose.runtime.Composable
import com.anlian.italase.shared.data.repository.ErrorCode
import com.anlian.italase.shared.util.formatRupiah
import italase.shared.generated.resources.Res
import italase.shared.generated.resources.err_amount_positive
import italase.shared.generated.resources.err_customer_required
import italase.shared.generated.resources.err_debt_missing
import italase.shared.generated.resources.err_debt_paid
import italase.shared.generated.resources.err_expense_name
import italase.shared.generated.resources.err_generic
import italase.shared.generated.resources.err_name_required
import italase.shared.generated.resources.err_price_positive
import italase.shared.generated.resources.err_summary_failed
import italase.shared.generated.resources.msg_cart_empty
import italase.shared.generated.resources.msg_checkout_failed
import italase.shared.generated.resources.msg_debt_paid
import italase.shared.generated.resources.msg_debt_pay_failed
import italase.shared.generated.resources.msg_debt_saved
import italase.shared.generated.resources.msg_debt_save_failed
import italase.shared.generated.resources.msg_delete_failed
import italase.shared.generated.resources.msg_exp_save_failed
import italase.shared.generated.resources.msg_exp_saved
import italase.shared.generated.resources.msg_product_gone
import italase.shared.generated.resources.msg_prod_deleted
import italase.shared.generated.resources.msg_prod_saved
import italase.shared.generated.resources.msg_qty_invalid
import italase.shared.generated.resources.msg_sale_saved
import italase.shared.generated.resources.msg_save_failed
import italase.shared.generated.resources.msg_stock_short
import italase.shared.generated.resources.msg_txn_saved
import org.jetbrains.compose.resources.stringResource

sealed interface UiMessage {
    data object ProductSaved : UiMessage
    data object ProductDeleted : UiMessage
    data object SaveFailed : UiMessage
    data object DeleteFailed : UiMessage
    data object NameRequired : UiMessage
    data object PricePositive : UiMessage
    data class StockShort(val name: String) : UiMessage
    data class TxnSaved(val id: Long) : UiMessage
    data object CheckoutFailed : UiMessage
    data object CartEmpty : UiMessage
    data class ProductGone(val name: String) : UiMessage
    data object QtyInvalid : UiMessage
    data object SummaryFailed : UiMessage
    data object DebtSaved : UiMessage
    data class DebtPaid(val amount: Long) : UiMessage
    data object DebtAlreadyPaid : UiMessage
    data object DebtSaveFailed : UiMessage
    data object DebtPayFailed : UiMessage
    data object CustomerRequired : UiMessage
    data object ExpenseNameRequired : UiMessage
    data object AmountPositive : UiMessage
    data object DebtMissing : UiMessage
    data object ExpenseSaved : UiMessage
    data object ExpenseSaveFailed : UiMessage
    data object SaleInfoSaved : UiMessage
    data object GenericFailure : UiMessage
}

fun ErrorCode.toMessage(arg: String = ""): UiMessage = when (this) {
    ErrorCode.EMPTY_CART -> UiMessage.CartEmpty
    ErrorCode.PRODUCT_GONE -> UiMessage.ProductGone(arg)
    ErrorCode.QTY_INVALID -> UiMessage.QtyInvalid
    ErrorCode.STOCK_SHORT -> UiMessage.StockShort(arg)
    ErrorCode.CUSTOMER_REQUIRED -> UiMessage.CustomerRequired
    ErrorCode.EXPENSE_NAME_REQUIRED -> UiMessage.ExpenseNameRequired
    ErrorCode.AMOUNT_POSITIVE -> UiMessage.AmountPositive
    ErrorCode.DEBT_MISSING -> UiMessage.DebtMissing
    ErrorCode.ALREADY_PAID -> UiMessage.DebtAlreadyPaid
}

@Composable
fun UiMessage.text(): String = when (this) {
    UiMessage.ProductSaved -> stringResource(Res.string.msg_prod_saved)
    UiMessage.ProductDeleted -> stringResource(Res.string.msg_prod_deleted)
    UiMessage.SaveFailed -> stringResource(Res.string.msg_save_failed)
    UiMessage.DeleteFailed -> stringResource(Res.string.msg_delete_failed)
    UiMessage.NameRequired -> stringResource(Res.string.err_name_required)
    UiMessage.PricePositive -> stringResource(Res.string.err_price_positive)
    is UiMessage.StockShort -> stringResource(Res.string.msg_stock_short, name)
    is UiMessage.TxnSaved -> stringResource(Res.string.msg_txn_saved, id)
    UiMessage.CheckoutFailed -> stringResource(Res.string.msg_checkout_failed)
    UiMessage.CartEmpty -> stringResource(Res.string.msg_cart_empty)
    is UiMessage.ProductGone -> stringResource(Res.string.msg_product_gone, name)
    UiMessage.QtyInvalid -> stringResource(Res.string.msg_qty_invalid)
    UiMessage.SummaryFailed -> stringResource(Res.string.err_summary_failed)
    UiMessage.DebtSaved -> stringResource(Res.string.msg_debt_saved)
    is UiMessage.DebtPaid -> stringResource(Res.string.msg_debt_paid, formatRupiah(amount))
    UiMessage.DebtAlreadyPaid -> stringResource(Res.string.err_debt_paid)
    UiMessage.DebtSaveFailed -> stringResource(Res.string.msg_debt_save_failed)
    UiMessage.DebtPayFailed -> stringResource(Res.string.msg_debt_pay_failed)
    UiMessage.CustomerRequired -> stringResource(Res.string.err_customer_required)
    UiMessage.ExpenseNameRequired -> stringResource(Res.string.err_expense_name)
    UiMessage.AmountPositive -> stringResource(Res.string.err_amount_positive)
    UiMessage.DebtMissing -> stringResource(Res.string.err_debt_missing)
    UiMessage.ExpenseSaved -> stringResource(Res.string.msg_exp_saved)
    UiMessage.ExpenseSaveFailed -> stringResource(Res.string.msg_exp_save_failed)
    UiMessage.SaleInfoSaved -> stringResource(Res.string.msg_sale_saved)
    UiMessage.GenericFailure -> stringResource(Res.string.err_generic)
}
