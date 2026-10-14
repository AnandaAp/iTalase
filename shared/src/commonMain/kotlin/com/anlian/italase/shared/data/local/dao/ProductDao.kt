package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM ${Tables.PRODUCTS} ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM ${Tables.PRODUCTS} WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM ${Tables.PRODUCTS} WHERE name LIKE '%' || :q || '%' OR barcode LIKE '%' || :q || '%' ORDER BY name ASC")
    suspend fun search(q: String): List<ProductEntity>

    @Query("SELECT * FROM ${Tables.PRODUCTS} WHERE stock <= minStock ORDER BY stock ASC")
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProductEntity): Long

    @Update
    suspend fun update(item: ProductEntity)

    @Delete
    suspend fun delete(item: ProductEntity)

    @Query("SELECT COUNT(*) FROM ${Tables.PRODUCTS}")
    suspend fun count(): Int
}
