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

/** Database tunggal aplikasi (file [DbFiles.NAME]) yang memuat seluruh tabel
 *  POS. Nomor versi skema sengaja tinggal di sini, bukan di Entity: setiap
 *  perubahan tabel WAJIB menaikkan `version` sekaligus menambah Migration,
 *  karena data kasir tidak boleh hilang saat aplikasi di-update. */
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
    /** Jalur baca/tulis tabel products; hasilnya tidak di-cache di sini. */
    abstract fun productDao(): ProductDao

    /** Jalur baca/tulis sales dan sale_items. */
    abstract fun saleDao(): SaleDao

    /** Jalur baca/tulis debts dan debt_payments. */
    abstract fun debtDao(): DebtDao

    /** Jalur baca/tulis expenses. */
    abstract fun expenseDao(): ExpenseDao
}

/** Versi skema 1: rilis pertama. */
private const val DB_VERSION_1 = 1

/** Versi skema 2: sales punya kolom tendered. */
private const val DB_VERSION_2 = 2

/** Versi skema 3: products dan sale_items punya kolom category. */
private const val DB_VERSION_3 = 3

/** Migrasi 1→2: menambah sales.tendered (uang diserahkan pembeli) sebagai
 *  kolom nullable, karena struk lama memang tidak menyimpan nilai itu. */
val MIGRATION_1_2 = object : Migration(DB_VERSION_1, DB_VERSION_2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE ${Tables.SALES} ADD COLUMN tendered INTEGER")
    }
}

/** Migrasi 2→3: menambah category di products dan sale_items. Keduanya
 *  NOT NULL dengan nilai default agar baris lama tetap valid tanpa diisi
 *  ulang; sale_items memakai `''` karena kategori baris itu tidak diketahui. */
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

/** Implementasi konkret per platform (lihat androidMain/iosMain) karena Room
 *  perlu kode pembangun database yang beda tiap target. */
@Suppress("KotlinNoActualForExpect")
expect object ITalaseDatabaseConstructor : RoomDatabaseConstructor<ITalaseDatabase> {
    /** Dipanggil oleh generator Room, bukan kode aplikasi; instance-nya
     *  diambil lewat DI (Koin), jangan dibuat manual di tempat lain. */
    override fun initialize(): ITalaseDatabase
}

/** Konfigurasi tunggal database: driver SQLite bundled (tidak bergantung
 *  sqlite sistem, jadi perilakunya sama di Android & iOS), query dijalankan di
 *  [Dispatchers.IO], dan seluruh migrasi didaftarkan. Dipanggil platform module
 *  saat menyiapkan DI. */
fun getRoomDatabase(builder: RoomDatabase.Builder<ITalaseDatabase>): ITalaseDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}
