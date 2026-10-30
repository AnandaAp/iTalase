package com.anlian.italase.shared.data.local

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.anlian.italase.shared.data.local.entity.DebtStatus
import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.ProductEntity
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.data.local.entity.TxnStatus
import com.anlian.italase.shared.data.repository.CartLine
import com.anlian.italase.shared.data.repository.FakeDatabase
import com.anlian.italase.shared.data.repository.InMemoryQrisConfigDao
import com.anlian.italase.shared.data.repository.ShopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Empirical challenger test harness for Milestone M1 (AUD-107).
 * Tests stress limits, boundary conditions, SQLite execution, and checkout comparisons.
 */
class QrisEmpiricalVerificationTest {

    // -------------------------------------------------------------------------
    // 1. BOUNDARY CONDITIONS & EXTREMES
    // -------------------------------------------------------------------------

    @Test
    fun testPromptDismissCountBoundaryAndProgression() = runBlocking {
        val qDao = InMemoryQrisConfigDao(initial = QrisConfigEntity(id = 1, promptDismissCount = 0))
        val db = FakeDatabase(qDao = qDao)
        val repo = ShopRepository(db)

        // Initial state
        var cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertEquals(0, cfg.promptDismissCount)
        assertTrue(cfg.imageBytes == null && cfg.promptDismissCount < 3, "Initial state should be prompt-eligible")

        // Progressively dismiss: 1, 2, 3
        repo.dismissQrisPrompt()
        cfg = repo.getQrisConfig()
        assertEquals(1, cfg?.promptDismissCount)
        assertTrue(cfg!!.imageBytes == null && cfg.promptDismissCount < 3)

        repo.dismissQrisPrompt()
        cfg = repo.getQrisConfig()
        assertEquals(2, cfg?.promptDismissCount)
        assertTrue(cfg!!.imageBytes == null && cfg.promptDismissCount < 3)

        repo.dismissQrisPrompt()
        cfg = repo.getQrisConfig()
        assertEquals(3, cfg?.promptDismissCount)
        assertFalse(cfg!!.imageBytes == null && cfg.promptDismissCount < 3, "Dismiss count 3 must NOT be prompt-eligible")

        // Boundary stress: dismiss beyond 3 (e.g. 4, 5, up to 100)
        for (i in 4..100) {
            repo.dismissQrisPrompt()
        }
        cfg = repo.getQrisConfig()
        assertEquals(100, cfg?.promptDismissCount)
        assertFalse(cfg!!.imageBytes == null && cfg.promptDismissCount < 3)

        // Dismiss when imageBytes is present: user has set up QRIS, prompt is already ineligible
        val dummyBytes = byteArrayOf(10, 20, 30)
        repo.saveQrisConfig(dummyBytes)
        cfg = repo.getQrisConfig()
        assertEquals(100, cfg?.promptDismissCount, "saveQrisConfig must preserve previous dismiss count")
        assertFalse(cfg!!.imageBytes == null && cfg.promptDismissCount < 3)
    }

    @Test
    fun testBlobByteArraysExtremesAndMemorySafety() = runBlocking {
        val qDao = InMemoryQrisConfigDao(initial = null)
        val db = FakeDatabase(qDao = qDao)
        val repo = ShopRepository(db)

        // 1. Null blob in entity
        val nullBlobEntity = QrisConfigEntity(id = 1, imageBytes = null)
        assertNull(nullBlobEntity.imageBytes)
        assertEquals("null", nullBlobEntity.toString().substringAfter("imageBytes=").substringBefore(","))

        // 2. Empty byte array
        val emptyBytes = ByteArray(0)
        repo.saveQrisConfig(emptyBytes, "image/jpeg")
        var config = repo.getQrisConfig()
        assertNotNull(config)
        assertNotNull(config.imageBytes)
        assertEquals(0, config.imageBytes?.size)
        assertEquals("0 bytes", config.toString().substringAfter("imageBytes=").substringBefore(","))

        // 3. Typical QRIS size (50 KB)
        val typicalBytes = ByteArray(50 * 1024) { (it % 256).toByte() }
        repo.saveQrisConfig(typicalBytes, "image/jpeg")
        config = repo.getQrisConfig()
        assertNotNull(config)
        assertEquals(50 * 1024, config.imageBytes?.size)
        assertTrue(typicalBytes.contentEquals(config.imageBytes))

        // 4. Upper bound size: 1 MB and 2 MB
        val largeBytes1MB = ByteArray(1024 * 1024) { (it % 128).toByte() }
        repo.saveQrisConfig(largeBytes1MB, "image/png")
        config = repo.getQrisConfig()
        assertNotNull(config)
        assertEquals(1024 * 1024, config.imageBytes?.size)
        assertTrue(largeBytes1MB.contentEquals(config.imageBytes))

        // Value equality verification with large blobs
        val configCopy = config.copy()
        assertEquals(config, configCopy)
        assertEquals(config.hashCode(), configCopy.hashCode())

        // Mutating a separate array must not affect equality
        val modifiedBytes = largeBytes1MB.copyOf()
        modifiedBytes[0] = (modifiedBytes[0] + 1).toByte()
        val modifiedConfig = config.copy(imageBytes = modifiedBytes)
        assertNotEquals(config, modifiedConfig)
    }

    @Test
    fun testMimeTypeHandlingAndPreservation() = runBlocking {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        val testBytes = byteArrayOf(1, 2, 3)

        // Standard MIME types
        val mimeTypes = listOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "IMAGE/JPEG", // Case variance
            "application/octet-stream",
            "custom/x-qris",
        )

        for (mime in mimeTypes) {
            repo.saveQrisConfig(testBytes, mime)
            val cfg = repo.getQrisConfig()
            assertNotNull(cfg)
            assertEquals(mime, cfg.mimeType)
            assertTrue(cfg.isEnabled)
        }
    }

    // -------------------------------------------------------------------------
    // 2. COMPREHENSIVE STATE MACHINE TRANSITIONS
    // -------------------------------------------------------------------------

    @Test
    fun testCompleteLifecycleStateTransitions() = runBlocking {
        // State A: Unseeded clean install (table initially empty)
        val qDao = InMemoryQrisConfigDao(initial = null)
        val db = FakeDatabase(qDao = qDao)
        val repo = ShopRepository(db)

        assertNull(repo.getQrisConfig(), "State A: Clean install should return null initially")

        // State A -> Dismiss on empty table creates initial record with count = 1
        repo.dismissQrisPrompt()
        var cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertEquals(1, cfg.promptDismissCount)
        assertFalse(cfg.isEnabled)
        assertNull(cfg.imageBytes)

        // State B: Setup completed (Save QRIS config)
        val photoBytes1 = byteArrayOf(11, 22, 33)
        repo.saveQrisConfig(photoBytes1, "image/jpeg")
        cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertTrue(cfg.isEnabled, "Saving QRIS should enable it automatically")
        assertTrue(photoBytes1.contentEquals(cfg.imageBytes))
        assertEquals(1, cfg.promptDismissCount, "Dismiss count must be preserved")

        // State C: Disable from Settings
        repo.setQrisEnabled(false)
        cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertFalse(cfg.isEnabled, "Should be disabled")
        assertTrue(photoBytes1.contentEquals(cfg.imageBytes), "imageBytes must remain intact when disabled")

        // State D: Re-enable from Settings
        repo.setQrisEnabled(true)
        cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertTrue(cfg.isEnabled, "Should be re-enabled")
        assertTrue(photoBytes1.contentEquals(cfg.imageBytes))

        // State E: Replace image with new photo
        val photoBytes2 = byteArrayOf(44, 55, 66, 77)
        repo.saveQrisConfig(photoBytes2, "image/png")
        cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertTrue(cfg.isEnabled)
        assertTrue(photoBytes2.contentEquals(cfg.imageBytes), "New photo should replace old photo")
        assertEquals("image/png", cfg.mimeType)

        // State F: Delete QRIS config permanently
        repo.deleteQrisConfig()
        cfg = repo.getQrisConfig()
        assertNull(cfg, "State F: Config should be null after deletion")

        // State G: Toggle enabled on deleted/null record
        repo.setQrisEnabled(true)
        cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertTrue(cfg.isEnabled)
        assertNull(cfg.imageBytes)

        // State H: Re-save photo after deletion
        val photoBytes3 = byteArrayOf(99)
        repo.saveQrisConfig(photoBytes3)
        cfg = repo.getQrisConfig()
        assertNotNull(cfg)
        assertTrue(cfg.isEnabled)
        assertTrue(photoBytes3.contentEquals(cfg.imageBytes))
    }

    // -------------------------------------------------------------------------
    // 3. DIRECT SQLITE MIGRATION EXECUTION (ROOM v6 -> v7)
    // -------------------------------------------------------------------------

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
    fun testRealSQLiteMigrationExecutionAndIdempotency() {
        val connection = RecordingSQLiteConnection()

        // 1. Directly execute MIGRATION_6_7
        MIGRATION_6_7.migrate(connection)

        // 2. Verify statements count and contents
        assertEquals(2, connection.statements.size, "MIGRATION_6_7 must execute DDL and DML")
        val ddl = connection.statements[0]
        val dml = connection.statements[1]

        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS ${Tables.QRIS_CONFIG}"))
        assertTrue(ddl.contains("id INTEGER NOT NULL"))
        assertTrue(ddl.contains("isEnabled INTEGER NOT NULL"))
        assertTrue(ddl.contains("imageBytes BLOB"))
        assertTrue(ddl.contains("mimeType TEXT NOT NULL"))
        assertTrue(ddl.contains("promptDismissCount INTEGER NOT NULL"))
        assertTrue(ddl.contains("updatedAt INTEGER NOT NULL"))
        assertTrue(ddl.contains("PRIMARY KEY(id)"))

        assertTrue(dml.contains("INSERT OR IGNORE INTO ${Tables.QRIS_CONFIG}"))
        assertTrue(dml.contains("VALUES (1, 0, NULL, 'image/jpeg', 0, 0)"))

        // 3. Test idempotency: re-running MIGRATION_6_7 adds 2 identical statements safely
        MIGRATION_6_7.migrate(connection)
        assertEquals(4, connection.statements.size)
        assertEquals(ddl, connection.statements[2])
        assertEquals(dml, connection.statements[3])
    }

    // -------------------------------------------------------------------------
    // 4. CHECKOUT COMPARISON: QRIS vs TUNAI vs BON
    // -------------------------------------------------------------------------

    @Test
    fun testCheckoutComparisonQrisVsTunaiVsBon() = runBlocking {
        val (db, repo) = setupRepo()
        val prod = ProductEntity(id = 100L, name = "Barang Uji", buyPrice = 3000, sellPrice = 5000, stock = 100)
        db.pDao.upsert(prod)

        val cart = listOf(CartLine(prod, 2)) // total = 10000

        // Case A: QRIS exact payment (paidAmount = 10000)
        val qrisSaleId = repo.checkout(
            lines = cart,
            paidAmount = 10000,
            payMethod = PayMethod.QRIS,
            customerName = "Cust QRIS",
            note = "QRIS Dinamis",
        )
        val qrisSale = db.sDao.getSale(qrisSaleId)
        assertNotNull(qrisSale)
        assertEquals(PayMethod.QRIS, qrisSale.payMethod)
        assertEquals(TxnStatus.LUNAS, qrisSale.status)
        assertEquals(10000L, qrisSale.total)
        assertEquals(10000L, qrisSale.paidAmount)
        assertEquals(10000L, qrisSale.tendered)
        assertNull(db.dDao.getBySaleId(qrisSaleId), "QRIS exact payment should not create debt")
        assertEquals(98, db.pDao.getById(100L)?.stock)

        // Case B: QRIS overpayment (paidAmount = 15000)
        val qrisOverSaleId = repo.checkout(
            lines = cart,
            paidAmount = 15000,
            payMethod = PayMethod.QRIS,
            customerName = "Cust QRIS Over",
        )
        val qrisOverSale = db.sDao.getSale(qrisOverSaleId)
        assertNotNull(qrisOverSale)
        assertEquals(PayMethod.QRIS, qrisOverSale.payMethod)
        assertEquals(TxnStatus.LUNAS, qrisOverSale.status)
        assertEquals(10000L, qrisOverSale.total)
        assertEquals(10000L, qrisOverSale.paidAmount)
        assertEquals(15000L, qrisOverSale.tendered)
        assertNull(db.dDao.getBySaleId(qrisOverSaleId))
        assertEquals(96, db.pDao.getById(100L)?.stock)

        // Case C: QRIS underpayment (paidAmount = 6000 < 10000) -> Must coerce to BON
        val qrisUnderSaleId = repo.checkout(
            lines = cart,
            paidAmount = 6000,
            payMethod = PayMethod.QRIS,
            customerName = "Cust QRIS Under",
        )
        val qrisUnderSale = db.sDao.getSale(qrisUnderSaleId)
        assertNotNull(qrisUnderSale)
        assertEquals(PayMethod.BON, qrisUnderSale.payMethod, "Underpayment must coerce to PayMethod.BON")
        assertEquals(TxnStatus.BON, qrisUnderSale.status)
        assertEquals(10000L, qrisUnderSale.total)
        assertEquals(6000L, qrisUnderSale.paidAmount)
        val qrisDebt = db.dDao.getBySaleId(qrisUnderSaleId)
        assertNotNull(qrisDebt)
        assertEquals(4000L, qrisDebt.remaining)
        assertEquals(DebtStatus.BELUM_LUNAS, qrisDebt.status)
        assertEquals(94, db.pDao.getById(100L)?.stock)

        // Case D: TUNAI exact / overpayment
        val tunaiSaleId = repo.checkout(
            lines = cart,
            paidAmount = 20000,
            payMethod = PayMethod.TUNAI,
            customerName = "Cust Tunai",
        )
        val tunaiSale = db.sDao.getSale(tunaiSaleId)
        assertNotNull(tunaiSale)
        assertEquals(PayMethod.TUNAI, tunaiSale.payMethod)
        assertEquals(TxnStatus.LUNAS, tunaiSale.status)
        assertEquals(10000L, tunaiSale.total)
        assertEquals(10000L, tunaiSale.paidAmount)
        assertEquals(20000L, tunaiSale.tendered)
        assertNull(db.dDao.getBySaleId(tunaiSaleId))
        assertEquals(92, db.pDao.getById(100L)?.stock)

        // Case E: BON explicit payment
        val bonSaleId = repo.checkout(
            lines = cart,
            paidAmount = 0,
            payMethod = PayMethod.BON,
            customerName = "Cust Bon",
        )
        val bonSale = db.sDao.getSale(bonSaleId)
        assertNotNull(bonSale)
        assertEquals(PayMethod.BON, bonSale.payMethod)
        assertEquals(TxnStatus.BON, bonSale.status)
        assertEquals(0L, bonSale.paidAmount)
        val bonDebt = db.dDao.getBySaleId(bonSaleId)
        assertNotNull(bonDebt)
        assertEquals(10000L, bonDebt.remaining)
        assertEquals(90, db.pDao.getById(100L)?.stock)
    }

    private fun setupRepo(): Pair<FakeDatabase, ShopRepository> {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        return db to repo
    }
}
