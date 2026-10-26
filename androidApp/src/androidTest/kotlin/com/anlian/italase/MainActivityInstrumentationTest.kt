package com.anlian.italase

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * On-device instrumentation test running on physical hardware (Samsung Galaxy A54).
 * Verifies application packaging, Application context, MainActivity lifecycle, and database storage path.
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
    fun testOnDeviceDatabasePath() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = appContext.getDatabasePath("italase.db")
        assertNotNull(dbFile)
        assertTrue(dbFile.absolutePath.endsWith("italase.db"))
    }
}
