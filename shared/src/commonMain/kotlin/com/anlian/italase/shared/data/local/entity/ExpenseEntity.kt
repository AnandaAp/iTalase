package com.anlian.italase.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.anlian.italase.shared.data.ShopDefaults
import com.anlian.italase.shared.data.local.Tables

@Entity(tableName = Tables.EXPENSES)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val category: String = ShopDefaults.CATEGORY,
    val amount: Long = 0,
    val dateMillis: Long = 0,
    val note: String = "",
)
