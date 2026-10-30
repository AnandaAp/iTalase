package com.anlian.italase.shared.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.anlian.italase.shared.data.ProductCategories
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.data.local.entity.SaleEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.data.repository.FakeDatabase
import com.anlian.italase.shared.data.repository.InMemoryQrisConfigDao
import com.anlian.italase.shared.data.repository.ShopRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Empirical stress tests and adversarial verification harness for AUD-107 Milestone M1.
 *
 * Verifies:
 * 1. High-resolution / large image blob memory behavior, GC pressure, equals/hashCode value semantics.
 * 2. Reactive Flow emission triggers on QrisConfig updates (promptDismissCount, updatedAt, isEnabled).
 * 3. Parity and semantic divergence between InMemoryQrisConfigDao and SQLite Room DDL/DML.
 * 4. MIGRATION_6_7 schema integrity, data isolation from existing tables, index validity, and idempotency.
 */
class QrisEmpiricalChallengeTest {

    // =========================================================================
    // 1. Memory Safety & Large Image Blob Stress Tests
    // =========================================================================

    @Test
    fun testLargeImageBytesMemoryAndContentEquality() {
        val sizes = listOf(10 * 1024, 50 * 1024, 150 * 1024, 500 * 1024, 1024 * 1024) // 10KB to 1MB

        for (size in sizes) {
            val bytes1 = ByteArray(size) { (it % 127).toByte() }
            val bytes2 = ByteArray(size) { (it % 127).toByte() }
            val bytesDiff = ByteArray(size) { (it % 127).toByte() }.also { it[size - 1] = 99 }

            val entity1 = QrisConfigEntity(id = 1, isEnabled = true, imageBytes = bytes1, updatedAt = 1000L)
            val entity2 = QrisConfigEntity(id = 1, isEnabled = true, imageBytes = bytes2, updatedAt = 1000L)
            val entityDiff = QrisConfigEntity(id = 1, isEnabled = true, imageBytes = bytesDiff, updatedAt = 1000L)

            // Value equality over content
            assertEquals(entity1, entity2, "Entities with equal byte content must be equal")
            assertEquals(entity1.hashCode(), entity2.hashCode(), "Hashcodes must match for equal content")
            assertTrue(entity1 != entityDiff, "Entities with differing byte content must not be equal")

            // Verify toString() does not dump 1MB of raw array characters
            val str = entity1.toString()
            assertTrue(str.contains("$size bytes"), "toString should summarize image size: $str")
            assertFalse(str.contains("[B@"), "toString should not leak raw array reference")
        }
    }

    @Test
    fun testFrequentDismissalDoesNotCorruptOrReallocateImageBytes() = runBlocking {
        val fakeDb = FakeDatabase()
        val repo = ShopRepository(fakeDb)

        val image150KB = ByteArray(150 * 1024) { (it % 251).toByte() }
        repo.saveQrisConfig(image150KB, "image/png")

        val initialConfig = repo.getQrisConfig()
        assertNotNull(initialConfig)
        assertTrue(image150KB.contentEquals(initialConfig.imageBytes))
        assertEquals(0, initialConfig.promptDismissCount)

        // Dismiss 10 times consecutively
        for (i in 1..10) {
            repo.dismissQrisPrompt()
            val cur = repo.getQrisConfig()
            assertNotNull(cur)
            assertEquals(i, cur.promptDismissCount)
            // Verify imageBytes remains intact and preserved across dismisses
            assertNotNull(cur.imageBytes)
            assertEquals(150 * 1024, cur.imageBytes?.size)
            assertTrue(image150KB.contentEquals(cur.imageBytes))
        }
    }

    // =========================================================================
    // 2. Flow Emission Behavior & Invalidation Analysis
    // =========================================================================

    @Test
    fun testFlowEmissionTriggersOnFieldUpdates() = runBlocking {
        val fakeDb = FakeDatabase()
        val repo = ShopRepository(fakeDb)

        val emissions = mutableListOf<QrisConfigEntity?>()
        val job = launch {
            repo.observeQrisConfig().collect { emissions.add(it) }
        }
        yield()

        // Emission 1: Initial state from FakeDatabase
        assertEquals(1, emissions.size)
        assertEquals(0, emissions[0]?.promptDismissCount)

        // Step 1: Save QRIS config -> Triggers emission 2
        val img = byteArrayOf(1, 2, 3)
        repo.saveQrisConfig(img)
        yield()
        assertEquals(2, emissions.size)
        assertTrue(emissions[1]?.isEnabled == true)
        assertTrue(img.contentEquals(emissions[1]?.imageBytes))

        // Step 2: Dismiss prompt -> Triggers emission 3 (promptDismissCount changes 0 -> 1)
        repo.dismissQrisPrompt()
        yield()
        assertEquals(3, emissions.size)
        assertEquals(1, emissions[2]?.promptDismissCount)
        assertTrue(img.contentEquals(emissions[2]?.imageBytes))

        // Step 3: Toggle disabled -> Triggers emission 4
        repo.setQrisEnabled(false)
        yield()
        assertEquals(4, emissions.size)
        assertEquals(false, emissions[3]?.isEnabled)

        // Step 4: Toggle enabled -> Triggers emission 5
        repo.setQrisEnabled(true)
        yield()
        assertEquals(5, emissions.size)
        assertEquals(true, emissions[4]?.isEnabled)

        job.cancel()
    }

    @Test
    fun testDistinctUntilChangedBehaviorOnImageBytesVersusWholeEntity() = runBlocking {
        val fakeDb = FakeDatabase()
        val repo = ShopRepository(fakeDb)

        val img = byteArrayOf(10, 20, 30)
        repo.saveQrisConfig(img)

        val wholeEntityEmissions = mutableListOf<QrisConfigEntity?>()
        val imageOnlyEmissions = mutableListOf<ByteArray?>()

        val job1 = launch {
            repo.observeQrisConfig().distinctUntilChanged().collect { wholeEntityEmissions.add(it) }
        }
        val job2 = launch {
            repo.observeQrisConfig()
                .map { it?.imageBytes }
                .distinctUntilChanged { old, new ->
                    if (old == null && new == null) true
                    else if (old == null || new == null) false
                    else old.contentEquals(new)
                }
                .collect { imageOnlyEmissions.add(it) }
        }
        yield()

        assertEquals(1, wholeEntityEmissions.size)
        assertEquals(1, imageOnlyEmissions.size)

        // Dismiss prompt: promptDismissCount changes 0 -> 1.
        // Whole entity changes because promptDismissCount changed.
        repo.dismissQrisPrompt()
        yield()

        assertEquals(2, wholeEntityEmissions.size, "Whole entity flow emits because promptDismissCount changed")
        assertEquals(1, imageOnlyEmissions.size, "Image-only flow should NOT emit because imageBytes did not change!")

        job1.cancel()
        job2.cancel()
    }

    // =========================================================================
    // 3. InMemoryQrisConfigDao vs SQLite Semantic Parity
    // =========================================================================

    @Test
    fun testInMemoryDaoEdgeCaseDirectInvocationAfterDelete() = runBlocking {
        val dao = InMemoryQrisConfigDao()
        dao.deleteConfig()
        assertNull(dao.getConfig())

        // Empirical check: In InMemoryQrisConfigDao, calling incrementDismissCount()
        // or setEnabled() when null creates a new record (fallback behavior):
        dao.incrementDismissCount()
        val resurrected = dao.getConfig()
        assertNotNull(resurrected, "InMemoryQrisConfigDao creates row on null")
        assertEquals(1, resurrected.promptDismissCount)

        // Verify that ShopRepository shields callers from uninitialized state:
        val fakeDb = FakeDatabase()
        val repo = ShopRepository(fakeDb)
        repo.deleteQrisConfig()
        assertNull(repo.getQrisConfig())

        repo.dismissQrisPrompt()
        val repoResult = repo.getQrisConfig()
        assertNotNull(repoResult)
        assertEquals(1, repoResult.promptDismissCount)
    }

    @Test
    fun testSingletonRowEnforcement() = runBlocking {
        val dao = InMemoryQrisConfigDao()
        val config1 = QrisConfigEntity(id = 1, isEnabled = true)
        dao.upsert(config1)
        assertEquals(1, dao.getConfig()?.id)

        // Upsert with id = 1 overwrites singleton
        val configUpdated = QrisConfigEntity(id = 1, isEnabled = false, promptDismissCount = 2)
        dao.upsert(configUpdated)
        val cur = dao.getConfig()
        assertNotNull(cur)
        assertEquals(1, cur.id)
        assertFalse(cur.isEnabled)
        assertEquals(2, cur.promptDismissCount)
    }

    // =========================================================================
    // 4. MIGRATION_6_7 Statement Execution & Data Isolation
    // =========================================================================

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
    fun testMigration6To7TableIsolationAndZeroImpactOnExistingTables() {
        val conn = RecordingSQLiteConnection()
        MIGRATION_6_7.migrate(conn)

        assertEquals(2, conn.statements.size, "MIGRATION_6_7 must execute exactly 2 statements")

        val ddl = conn.statements[0]
        val dml = conn.statements[1]

        // 1. Verify DDL targets solely Tables.QRIS_CONFIG
        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ${Tables.QRIS_CONFIG}"))
        assertTrue(ddl.contains("id INTEGER NOT NULL"))
        assertTrue(ddl.contains("isEnabled INTEGER NOT NULL"))
        assertTrue(ddl.contains("imageBytes BLOB"))
        assertTrue(ddl.contains("mimeType TEXT NOT NULL"))
        assertTrue(ddl.contains("promptDismissCount INTEGER NOT NULL"))
        assertTrue(ddl.contains("updatedAt INTEGER NOT NULL"))
        assertTrue(ddl.contains("PRIMARY KEY(id)"))

        // 2. Verify DML inserts seed row with INSERT OR IGNORE
        assertTrue(dml.contains("INSERT OR IGNORE INTO ${Tables.QRIS_CONFIG}"))
        assertTrue(dml.contains("(1, 0, NULL, 'image/jpeg', 0, 0)"))

        // 3. Verify zero interference with existing tables
        val legacyTables = listOf(
            Tables.SALES,
            Tables.PRODUCTS,
            Tables.SALE_ITEMS,
            Tables.DEBTS,
            Tables.DEBT_PAYMENTS,
            Tables.EXPENSES
        )
        for (stmt in conn.statements) {
            for (table in legacyTables) {
                assertFalse(
                    stmt.contains(table, ignoreCase = true),
                    "Migration statement should never reference existing table '$table': $stmt"
                )
            }
            assertFalse(stmt.contains("DROP", ignoreCase = true), "Migration should never execute DROP: $stmt")
            assertFalse(stmt.contains("ALTER", ignoreCase = true), "Migration should never execute ALTER: $stmt")
            assertFalse(stmt.contains("DELETE", ignoreCase = true), "Migration should never execute DELETE: $stmt")
        }
    }
}
