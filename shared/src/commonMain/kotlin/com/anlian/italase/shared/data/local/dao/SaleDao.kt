package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM ${Tables.SALES} ORDER BY dateMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<SaleEntity>>

    @Query("SELECT * FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to ORDER BY dateMillis DESC")
    fun observeRange(from: Long, to: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to ORDER BY dateMillis DESC")
    suspend fun listRange(from: Long, to: Long): List<SaleEntity>

    @Query("SELECT IFNULL(SUM(total),0) FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun sumOmzet(from: Long, to: Long): Long

    @Query("SELECT IFNULL(SUM(profit),0) FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to")
    suspend fun sumProfit(from: Long, to: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT * FROM ${Tables.SALES} WHERE id = :id")
    suspend fun getSale(id: Long): SaleEntity?

    @Query("SELECT * FROM ${Tables.SALES} WHERE id IN (SELECT DISTINCT saleId FROM ${Tables.SALE_ITEMS} WHERE category = :category) ORDER BY dateMillis DESC LIMIT :limit")
    fun observeSalesByCategory(category: String, limit: Int = 200): Flow<List<SaleEntity>>

    @Query("SELECT DISTINCT category FROM ${Tables.SALE_ITEMS} WHERE category <> '' ORDER BY category ASC")
    fun observeItemCategories(): Flow<List<String>>

    @Query("UPDATE ${Tables.SALES} SET customerName = :customer, note = :note WHERE id = :id")
    suspend fun updateInfo(id: Long, customer: String, note: String)

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM ${Tables.SALE_ITEMS} WHERE saleId = :saleId")
    suspend fun itemsOf(saleId: Long): List<SaleItemEntity>

    @Query("SELECT productName, SUM(qty) as q, SUM(subtotal) as s FROM ${Tables.SALE_ITEMS} WHERE saleId IN (SELECT id FROM ${Tables.SALES} WHERE dateMillis BETWEEN :from AND :to) GROUP BY productName ORDER BY q DESC LIMIT 10")
    suspend fun topProducts(from: Long, to: Long): List<TopRow>

    @Query("DELETE FROM ${Tables.SALES} WHERE id = :id")
    suspend fun deleteSale(id: Long)

    @Query("DELETE FROM ${Tables.SALE_ITEMS} WHERE saleId = :saleId")
    suspend fun deleteItems(saleId: Long)
}

data class TopRow(
    val productName: String,
    val q: Int,
    val s: Long,
)
