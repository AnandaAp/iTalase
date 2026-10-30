package com.anlian.italase.shared.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.local.dao.DebtDao
import com.anlian.italase.shared.data.local.dao.ExpenseDao
import com.anlian.italase.shared.data.local.dao.ProductDao
import com.anlian.italase.shared.data.local.dao.QrisConfigDao
import com.anlian.italase.shared.data.local.dao.SaleDao
import com.anlian.italase.shared.data.local.entity.DebtEntity
import com.anlian.italase.shared.data.local.entity.DebtPaymentEntity
import com.anlian.italase.shared.data.local.entity.ExpenseEntity
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
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
        QrisConfigEntity::class,
    ],
    version = 7,
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

    /** Jalur baca/tulis konfigurasi QRIS toko. */
    abstract fun qrisConfigDao(): QrisConfigDao
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

/** Versi skema 4: indeks pada kolom filter dan foreign key. */
private const val DB_VERSION_4 = 4

/** Versi skema 5: indeks pada debts.saleId untuk percepatan kueri getBySaleId. */
private const val DB_VERSION_5 = 5

/** Versi skema 6: indeks komposit pada sales(dateMillis, status) dan products(name), products(category, name). */
private const val DB_VERSION_6 = 6

/** Migrasi 3→4: membuat indeks pada dateMillis, saleId, category, status, debtId. */
val MIGRATION_3_4 = object : Migration(DB_VERSION_3, DB_VERSION_4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_sales_dateMillis ON ${Tables.SALES}(dateMillis)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_saleId ON ${Tables.SALE_ITEMS}(saleId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_category ON ${Tables.SALE_ITEMS}(category)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_debts_status ON ${Tables.DEBTS}(status)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_debts_dateMillis ON ${Tables.DEBTS}(dateMillis)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON ${Tables.DEBT_PAYMENTS}(debtId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_dateMillis ON ${Tables.EXPENSES}(dateMillis)")
    }
}

/** Migrasi 4→5: membuat indeks pada debts.saleId. */
val MIGRATION_4_5 = object : Migration(DB_VERSION_4, DB_VERSION_5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_debts_saleId ON ${Tables.DEBTS}(saleId)")
    }
}

/** Migrasi 5→6: membuat indeks komposit pada sales(dateMillis, status) dan products(name), products(category, name). */
val MIGRATION_5_6 = object : Migration(DB_VERSION_5, DB_VERSION_6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_sales_dateMillis_status ON ${Tables.SALES}(dateMillis, status)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_products_name ON ${Tables.PRODUCTS}(name)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_products_category_name ON ${Tables.PRODUCTS}(category, name)")
    }
}

/** Versi skema 7: tabel konfigurasi QRIS kasir (AUD-107). */
private const val DB_VERSION_7 = 7

/** Migrasi 6→7: membuat tabel qris_config dan inisialisasi baris default (id=1). */
val MIGRATION_6_7 = object : Migration(DB_VERSION_6, DB_VERSION_7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${Tables.QRIS_CONFIG} (
                id INTEGER NOT NULL,
                isEnabled INTEGER NOT NULL,
                imageBytes BLOB,
                mimeType TEXT NOT NULL,
                promptDismissCount INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT OR IGNORE INTO ${Tables.QRIS_CONFIG} (id, isEnabled, imageBytes, mimeType, promptDismissCount, updatedAt)
            VALUES (1, 0, NULL, 'image/jpeg', 0, 0)
            """.trimIndent(),
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

/** Konfigurasi tunggal database: query dijalankan di [Dispatchers.IO], dan seluruh
 *  migrasi didaftarkan. Dipanggil platform module saat menyiapkan DI.
 *
 *  Driver-nya datang dari per-platform (`createEncryptedDriver`): Android memakai
 *  SQLCipher, iOS memakai SQLCipher lewat CocoaPods. Driver SQLite polos tidak
 *  dipakai di mana pun — database yang tersimpan di disk harus terenkripsi. */
/** Seluruh migrasi database terurut dari v1 hingga versi saat ini. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
)

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ITalaseDatabase>,
    driver: SQLiteDriver,
): ITalaseDatabase {
    return builder
        .setDriver(driver)
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}

/**
 * Driver terenkripsi untuk platform ini.
 *
 * Harus meng-key setiap koneksi yang dibuka, sebelum statement apa pun
 * dijalankan: Room membaca versi skema sebelum statement aplikasi pertama.
 */
expect fun createEncryptedDriver(passphrase: String): SQLiteDriver
