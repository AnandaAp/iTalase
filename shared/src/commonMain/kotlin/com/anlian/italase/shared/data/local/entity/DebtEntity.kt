package com.anlian.italase.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.local.Tables

object DebtStatus {
    const val BELUM_LUNAS: String = "BELUM_LUNAS"
    const val LUNAS: String = "LUNAS"
}

@Entity(tableName = Tables.DEBTS)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String = "",
    val phone: String = "",
    val saleId: Long? = null,
    val totalAmount: Long = 0,
    val paidAmount: Long = 0,
    val remaining: Long = 0,
    val status: String = DebtStatus.BELUM_LUNAS,
    val dateMillis: Long = 0,
    val note: String = "",
)

@Entity(tableName = Tables.DEBT_PAYMENTS)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long = 0,
    val amount: Long = 0,
    val dateMillis: Long = 0,
    val note: String = "",
)
