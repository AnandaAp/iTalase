package com.anlian.italase.shared.data.repository

enum class ErrorCode {
    EMPTY_CART,
    PRODUCT_GONE,
    QTY_INVALID,
    STOCK_SHORT,
    CUSTOMER_REQUIRED,
    EXPENSE_NAME_REQUIRED,
    AMOUNT_POSITIVE,
    DEBT_MISSING,
    ALREADY_PAID,
}

class ShopException(val code: ErrorCode, val arg: String = "") : IllegalArgumentException(code.name)
