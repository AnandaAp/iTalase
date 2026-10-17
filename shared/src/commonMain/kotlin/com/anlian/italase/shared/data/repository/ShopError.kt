package com.anlian.italase.shared.data.repository

/** Penyebab gagal validasi yang bisa diperbaiki kasir, jadi UI boleh
 *  menampilkan pesan berbeda per kode alih-alih satu pesan generik. */
enum class ErrorCode {
    /** Tombol bayar ditekan tanpa baris keranjang. */
    EMPTY_CART,

    /** Produk hilang dari tabel antara masuk keranjang dan checkout. */
    PRODUCT_GONE,

    /** qty baris keranjang <= 0. */
    QTY_INVALID,

    /** Stok tersisa kurang dari qty yang diminta. */
    STOCK_SHORT,

    /** Bon dibuat tanpa nama pelanggan. */
    CUSTOMER_REQUIRED,

    /** Pengeluaran dibuat tanpa nama. */
    EXPENSE_NAME_REQUIRED,

    /** Nominal <= 0 (pengeluaran, bon manual, atau cicilan bon). */
    AMOUNT_POSITIVE,

    /** id bon tidak ada di tabel debts. */
    DEBT_MISSING,

    /** Bon sudah lunas, jadi tidak bisa dicicil lagi. */
    ALREADY_PAID,
}

/** Kegagalan yang disebabkan input kasir, bukan kerusakan data: UI menangkapnya
 *  dan menampilkan pesan, bukan crash. Kesalahan lain sengaja dibiarkan naik
 *  sebagai exception biasa supaya tidak tersembunyi sebagai "input salah".
 *
 *  @param arg konteks tambahan untuk pesan — mis. nama produk pada
 *  [ErrorCode.PRODUCT_GONE] / [ErrorCode.STOCK_SHORT]. */
class ShopException(val code: ErrorCode, val arg: String = "") : IllegalArgumentException(code.name)
