package com.anlian.italase.shared.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.local.dao.DebtDao
import com.anlian.italase.shared.data.local.dao.ExpenseDao
import com.anlian.italase.shared.data.local.dao.ProductDao
import com.anlian.italase.shared.data.local.dao.SaleDao
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.SaleItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        ExpenseEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(ITalaseDatabaseConstructor::class)
abstract class ITalaseDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun debtDao(): DebtDao
    abstract fun expenseDao(): ExpenseDao
}

private const val DB_VERSION_1 = 1
private const val DB_VERSION_2 = 2
private const val DB_VERSION_3 = 3

val MIGRATION_1_2 = object : Migration(DB_VERSION_1, DB_VERSION_2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ${Tables.SALES} ADD COLUMN tendered INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(DB_VERSION_2, DB_VERSION_3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE ${Tables.PRODUCTS} ADD COLUMN category TEXT NOT NULL DEFAULT '${ProductCategories.OTHER}'",
        )
        connection.execSQL(
            "ALTER TABLE ${Tables.SALE_ITEMS} ADD COLUMN category TEXT NOT NULL DEFAULT ''",
        )
    }
}

@Suppress("KotlinNoActualForExpect")
expect object ITalaseDatabaseConstructor : RoomDatabaseConstructor<ITalaseDatabase> {
    override fun initialize(): ITalaseDatabase
}

fun getRoomDatabase(builder: RoomDatabase.Builder<ITalaseDatabase>): ITalaseDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}
