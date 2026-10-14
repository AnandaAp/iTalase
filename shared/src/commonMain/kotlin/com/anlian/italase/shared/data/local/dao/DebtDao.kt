package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM ${Tables.DEBTS} ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM ${Tables.DEBTS} WHERE status = '${DebtStatus.BELUM_LUNAS}' ORDER BY dateMillis DESC")
    fun observeActive(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM ${Tables.DEBTS} WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Insert
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("SELECT IFNULL(SUM(remaining),0) FROM ${Tables.DEBTS} WHERE status = '${DebtStatus.BELUM_LUNAS}'")
    suspend fun totalPiutang(): Long

    @Insert
    suspend fun insertPayment(p: DebtPaymentEntity): Long

    @Query("SELECT * FROM ${Tables.DEBT_PAYMENTS} WHERE debtId = :debtId ORDER BY dateMillis DESC")
    suspend fun paymentsOf(debtId: Long): List<DebtPaymentEntity>

    @Query("DELETE FROM ${Tables.DEBTS} WHERE id = :id")
    suspend fun delete(id: Long)
}
