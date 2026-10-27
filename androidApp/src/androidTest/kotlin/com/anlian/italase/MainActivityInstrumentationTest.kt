package com.anlian.italase

import android.content.pm.ActivityInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * On-device instrumentation test running on physical hardware (Samsung Galaxy A54).
 * Verifies application packaging, Application context, MainActivity lifecycle,
 * orientation landscape/portrait stability, and SQLCipher encrypted database integrity.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentationTest {

    @Test
    fun testApplicationContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.anlian.italase", appContext.packageName)

        val app = ApplicationProvider.getApplicationContext<ITalaseApplication>()
        assertNotNull(app)
    }

    @Test
    fun testMainActivityLaunchAndResume() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse(activity.isFinishing)
                assertNotNull(activity.window)
                assertNotNull(activity.window.decorView)
            }
        }
    }

    @Test
    fun testDeviceOrientationLandscape() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse(activity.isFinishing)
                val width = activity.resources.displayMetrics.widthPixels
                val height = activity.resources.displayMetrics.heightPixels
                assertTrue(width >= height, "Width ($width) should be >= height ($height) in landscape")
            }
        }
    }

    @Test
    fun testDeviceOrientationRotationCycle() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            // Rotate to Landscape
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)

            // Rotate back to Portrait
            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)

            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertFalse(activity.isFinishing)
            }
        }
    }

    @Test
    fun testOnDeviceDatabasePathAndEncryption() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = appContext.getDatabasePath("italase.db")
        assertNotNull(dbFile)
        assertTrue(dbFile.absolutePath.endsWith("italase.db"))

        if (dbFile.exists() && dbFile.length() >= 16) {
            val header = ByteArray(16)
            FileInputStream(dbFile).use { it.read(header) }
            val headerString = String(header, Charsets.US_ASCII)
            // SQLCipher encrypted database MUST NOT start with plaintext SQLite magic header
            assertNotEquals("SQLite format 3\u0000", headerString, "Database should be SQLCipher-encrypted, not raw plaintext")
        }
    }
}
