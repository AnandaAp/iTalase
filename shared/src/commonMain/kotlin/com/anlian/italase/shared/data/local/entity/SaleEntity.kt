package com.anlian.italase.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.local.Tables

object TxnStatus {
    const val LUNAS: String = "LUNAS"
    const val BON: String = "BON"
}

object PayMethod {
    const val TUNAI: String = "TUNAI"
    const val BON: String = "BON"
    const val TRANSFER: String = "TRANSFER"
}

@Entity(tableName = Tables.SALES)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long = 0,
    val total: Long = 0,
    val profit: Long = 0,
    val payMethod: String = PayMethod.TUNAI,
    val paidAmount: Long = 0,
    // Uang yang diserahkan pembeli (untuk hitung kembalian).
    // Null pada struk lama versi database 1.
    val tendered: Long? = null,
    val customerName: String = "",
    val status: String = TxnStatus.LUNAS,
    val note: String = "",
)

@Entity(tableName = Tables.SALE_ITEMS)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val category: String = "",
    val qty: Int = 0,
    val buyPrice: Long = 0,
    val sellPrice: Long = 0,
    val subtotal: Long = 0,
    val profit: Long = 0,
)
