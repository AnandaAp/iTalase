package com.anlian.italase.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.Tables

@Entity(tableName = Tables.PRODUCTS)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = ProductCategories.OTHER,
    val buyPrice: Long = 0,
    val sellPrice: Long = 0,
    val stock: Int = 0,
    val unit: String = ShopDefaults.UNIT,
    val minStock: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
