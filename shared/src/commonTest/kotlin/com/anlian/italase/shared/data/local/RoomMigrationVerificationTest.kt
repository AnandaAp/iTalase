package com.anlian.italase.shared.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.anlian.italase.shared.data.ProductCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifikasi konsistensi Room Auto-Migration & skema database v1 -> v6 (AUD-01, AUD-11 & CARD-101).
 * Menguji nomor versi skema, pendaftaran migrasi, dan eksekusi skrip DDL SQL.
 */
class RoomMigrationVerificationTest {

    private class RecordingSQLiteStatement(private val onStep: () -> Unit) : SQLiteStatement {
        override fun step(): Boolean {
            onStep()
            return false
        }
        override fun reset() {}
        override fun clearBindings() {}
        override fun close() {}
        override fun bindBlob(index: Int, value: ByteArray) {}
        override fun bindDouble(index: Int, value: Double) {}
        override fun bindLong(index: Int, value: Long) {}
        override fun bindNull(index: Int) {}
        override fun bindText(index: Int, value: String) {}
        override fun getBlob(index: Int): ByteArray = ByteArray(0)
        override fun getDouble(index: Int): Double = 0.0
        override fun getLong(index: Int): Long = 0L
        override fun getText(index: Int): String = ""
        override fun isNull(index: Int): Boolean = true
        override fun getColumnCount(): Int = 0
        override fun getColumnName(index: Int): String = ""
        override fun getColumnType(index: Int): Int = 0
    }

    private class RecordingSQLiteConnection : SQLiteConnection {
        val statements = mutableListOf<String>()

        override fun prepare(sql: String): SQLiteStatement {
            return RecordingSQLiteStatement {
                statements.add(sql)
            }
        }

        override fun close() {}
    }

    @Test
    fun testMigrationVersionsSequence() {
        // v1 -> v2
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)

        // v2 -> v3
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)

        // v3 -> v4
        assertEquals(3, MIGRATION_3_4.startVersion)
        assertEquals(4, MIGRATION_3_4.endVersion)

        // v4 -> v5 (AUD-11)
        assertEquals(4, MIGRATION_4_5.startVersion)
        assertEquals(5, MIGRATION_4_5.endVersion)

        // v5 -> v6 (CARD-101)
        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)

        // v6 -> v7 (AUD-107)
        assertEquals(6, MIGRATION_6_7.startVersion)
        assertEquals(7, MIGRATION_6_7.endVersion)
    }

    @Test
    fun testMigration1To2Execution() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_1_2.migrate(conn)

        assertEquals(1, conn.statements.size)
        assertEquals(
            "ALTER TABLE ${Tables.SALES} ADD COLUMN tendered INTEGER",
            conn.statements[0]
        )
    }

    @Test
    fun testMigration2To3Execution() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_2_3.migrate(conn)

        assertEquals(2, conn.statements.size)
        assertEquals(
            "ALTER TABLE ${Tables.PRODUCTS} ADD COLUMN category TEXT NOT NULL DEFAULT '${ProductCategories.OTHER}'",
            conn.statements[0]
        )
        assertEquals(
            "ALTER TABLE ${Tables.SALE_ITEMS} ADD COLUMN category TEXT NOT NULL DEFAULT ''",
            conn.statements[1]
        )
    }

    @Test
    fun testMigration3To4Execution() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_3_4.migrate(conn)

        assertEquals(7, conn.statements.size)
        assertTrue(conn.statements.any { it.contains("index_sales_dateMillis") })
        assertTrue(conn.statements.any { it.contains("index_sale_items_saleId") })
        assertTrue(conn.statements.any { it.contains("index_sale_items_category") })
        assertTrue(conn.statements.any { it.contains("index_debts_status") })
        assertTrue(conn.statements.any { it.contains("index_debts_dateMillis") })
        assertTrue(conn.statements.any { it.contains("index_debt_payments_debtId") })
        assertTrue(conn.statements.any { it.contains("index_expenses_dateMillis") })
    }

    @Test
    fun testMigration4To5ExecutionForDebtSaleIdIndex() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_4_5.migrate(conn)

        assertEquals(1, conn.statements.size)
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_debts_saleId ON ${Tables.DEBTS}(saleId)",
            conn.statements[0]
        )
    }

    @Test
    fun testMigration5To6ExecutionForCompositeIndexes() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_5_6.migrate(conn)

        assertEquals(3, conn.statements.size)
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_sales_dateMillis_status ON ${Tables.SALES}(dateMillis, status)",
            conn.statements[0]
        )
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_products_name ON ${Tables.PRODUCTS}(name)",
            conn.statements[1]
        )
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_products_category_name ON ${Tables.PRODUCTS}(category, name)",
            conn.statements[2]
        )
    }

    @Test
    fun testMigration6To7Execution() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_6_7.migrate(conn)

        assertEquals(2, conn.statements.size)
        assertTrue(conn.statements[0].contains("CREATE TABLE IF NOT EXISTS ${Tables.QRIS_CONFIG}"))
        assertTrue(conn.statements[0].contains("imageBytes BLOB"))
        assertTrue(conn.statements[0].contains("promptDismissCount INTEGER NOT NULL"))
        assertTrue(conn.statements[1].contains("INSERT OR IGNORE INTO ${Tables.QRIS_CONFIG}"))
    }

    @Test
    fun testFullSequentialMigrationPipelineFromV1ToV7() {
        val conn = RecordingSQLiteConnection()

        MIGRATION_1_2.migrate(conn)
        MIGRATION_2_3.migrate(conn)
        MIGRATION_3_4.migrate(conn)
        MIGRATION_4_5.migrate(conn)
        MIGRATION_5_6.migrate(conn)
        MIGRATION_6_7.migrate(conn)

        // 1 (v1->v2) + 2 (v2->v3) + 7 (v3->v4) + 1 (v4->v5) + 3 (v5->v6) + 2 (v6->v7) = 16 DDL/DML statements
        assertEquals(16, conn.statements.size)
        assertTrue(conn.statements.last().contains("INSERT OR IGNORE INTO ${Tables.QRIS_CONFIG}"))
    }
}
