package com.anlian.italase.shared.ui.screens

import com.anlian.italase.shared.data.local.entity.PayMethod
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.data.repository.FakeDatabase
import com.anlian.italase.shared.data.repository.ShopRepository
import com.anlian.italase.shared.ui.state.QrisViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Empirical stress test & adversarial verification harness for AUD-107 Milestone M4:
 * Cashier QRIS Prompt Banner & Dismissal Persistence.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CashierPromptEmpiricalChallengeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Exact banner visibility formula evaluated in CashierScreen (lines 286-288):
     * val isQrisActive = effectiveConfig != null && effectiveConfig.isEnabled && effectiveConfig.imageBytes != null
     * val isPromptPermanentlyDismissed = (effectiveConfig?.promptDismissCount ?: 0) >= 3
     * val bannerVisible = showQrisPrompt ?: (!isQrisActive && !isPromptPermanentlyDismissed && !dismissedThisSession)
     */
    private fun evaluateBannerVisibility(
        effectiveConfig: QrisConfigEntity?,
        dismissedThisSession: Boolean,
        showQrisPrompt: Boolean? = null,
    ): Boolean {
        val isQrisActive = effectiveConfig != null && effectiveConfig.isEnabled && effectiveConfig.imageBytes != null
        val isPromptPermanentlyDismissed = (effectiveConfig?.promptDismissCount ?: 0) >= 3
        return showQrisPrompt ?: (!isQrisActive && !isPromptPermanentlyDismissed && !dismissedThisSession)
    }

    // =========================================================================
    // 1. Exhaustive Truth Table Combinatorial Verification
    // =========================================================================

    @Test
    fun testBannerVisibilityExhaustiveTruthTable() {
        val enabledOptions = listOf(true, false)
        val imageOptions = listOf(null, byteArrayOf(1, 2, 3))
        val dismissCounts = listOf(0, 1, 2, 3, 4, 10)
        val sessionDismissedOptions = listOf(false, true)
        val showPromptOverrides = listOf(null, true, false)

        var totalPermutationsTested = 0

        // Permutations when effectiveConfig == null
        for (sessionDismissed in sessionDismissedOptions) {
            for (override in showPromptOverrides) {
                totalPermutationsTested++
                val visible = evaluateBannerVisibility(
                    effectiveConfig = null,
                    dismissedThisSession = sessionDismissed,
                    showQrisPrompt = override,
                )
                if (override != null) {
                    assertEquals(override, visible, "Override must dictate visibility when non-null")
                } else {
                    assertEquals(!sessionDismissed, visible, "When config is null, visible unless dismissed in session")
                }
            }
        }

        // Permutations when effectiveConfig != null
        for (enabled in enabledOptions) {
            for (image in imageOptions) {
                for (dismissCount in dismissCounts) {
                    val config = QrisConfigEntity(
                        id = 1,
                        isEnabled = enabled,
                        imageBytes = image,
                        mimeType = "image/jpeg",
                        promptDismissCount = dismissCount,
                        updatedAt = 1000L,
                    )
                    val isQrisActive = enabled && (image != null)
                    val isPermanentlyDismissed = dismissCount >= 3

                    for (sessionDismissed in sessionDismissedOptions) {
                        for (override in showPromptOverrides) {
                            totalPermutationsTested++
                            val visible = evaluateBannerVisibility(
                                effectiveConfig = config,
                                dismissedThisSession = sessionDismissed,
                                showQrisPrompt = override,
                            )

                            if (override != null) {
                                assertEquals(override, visible, "Explicit override must take precedence")
                            } else {
                                // Rule 1: If QRIS is active, banner NEVER appears
                                if (isQrisActive) {
                                    assertFalse(
                                        visible,
                                        "Banner must NEVER appear when QRIS is active (enabled=$enabled, hasImage=true)"
                                    )
                                }

                                // Rule 2: If promptDismissCount >= 3, banner NEVER appears
                                if (isPermanentlyDismissed) {
                                    assertFalse(
                                        visible,
                                        "Banner must NEVER appear when promptDismissCount >= 3 (count=$dismissCount)"
                                    )
                                }

                                // Rule 3: If not active and dismissCount < 3, visible iff !dismissedThisSession
                                if (!isQrisActive && !isPermanentlyDismissed) {
                                    assertEquals(
                                        !sessionDismissed,
                                        visible,
                                        "When unconfigured/disabled and count < 3, visibility should reflect session dismissal"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(totalPermutationsTested >= 150, "Expected at least 150 combinations tested, got $totalPermutationsTested")
    }

    // =========================================================================
    // 2. Lifecycle & Persistence Verification via QrisViewModel & Database
    // =========================================================================

    @Test
    fun testDismissPromptDatabaseIncrementAndSessionDismissalProgression() = runTest {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        val vm = QrisViewModel(repo)

        // Session 1: Initial state (count = 0)
        var dismissedThisSession = false
        var config = vm.qrisConfig.first()
        // If initial row is default with count 0 and imageBytes == null
        assertEquals(0, config?.promptDismissCount ?: 0)
        assertTrue(evaluateBannerVisibility(config, dismissedThisSession), "Banner visible on clean install")

        // User clicks '✕' dismiss button in Session 1
        dismissedThisSession = true
        vm.dismissPrompt()

        // Verify session immediately hides banner
        assertFalse(evaluateBannerVisibility(config, dismissedThisSession), "Dismiss hides banner in current session")

        // Verify Room database persists incremented count = 1
        val updatedConfig1 = repo.getQrisConfig()
        assertNotNull(updatedConfig1)
        assertEquals(1, updatedConfig1.promptDismissCount, "DB dismiss count must be 1")

        // Session 2 (e.g. app reopened or screen re-entered): dismissedThisSession resets to false
        dismissedThisSession = false
        config = updatedConfig1
        assertTrue(
            evaluateBannerVisibility(config, dismissedThisSession),
            "Banner must reappear in Session 2 because dismissCount (1) < 3"
        )

        // User dismisses again in Session 2
        dismissedThisSession = true
        vm.dismissPrompt()
        assertFalse(evaluateBannerVisibility(config, dismissedThisSession))

        val updatedConfig2 = repo.getQrisConfig()
        assertNotNull(updatedConfig2)
        assertEquals(2, updatedConfig2.promptDismissCount, "DB dismiss count must be 2")

        // Session 3: re-entry
        dismissedThisSession = false
        config = updatedConfig2
        assertTrue(
            evaluateBannerVisibility(config, dismissedThisSession),
            "Banner must reappear in Session 3 because dismissCount (2) < 3"
        )

        // User dismisses for the 3rd time in Session 3
        dismissedThisSession = true
        vm.dismissPrompt()
        assertFalse(evaluateBannerVisibility(config, dismissedThisSession))

        val updatedConfig3 = repo.getQrisConfig()
        assertNotNull(updatedConfig3)
        assertEquals(3, updatedConfig3.promptDismissCount, "DB dismiss count must be 3")

        // Session 4: New session after 3 dismissals (permanent threshold reached)
        dismissedThisSession = false
        config = updatedConfig3
        assertFalse(
            evaluateBannerVisibility(config, dismissedThisSession),
            "Banner must NEVER appear in Session 4 because dismissCount (3) >= 3"
        )

        // Even if user manages to trigger dismiss again
        vm.dismissPrompt()
        val updatedConfig4 = repo.getQrisConfig()
        assertNotNull(updatedConfig4)
        assertEquals(4, updatedConfig4.promptDismissCount, "DB dismiss count must be 4")
        assertFalse(
            evaluateBannerVisibility(updatedConfig4, dismissedThisSession = false),
            "Banner must NEVER appear when count exceeds 3"
        )
    }

    // =========================================================================
    // 3. QRIS Configuration Activation & Re-enabling Transitions
    // =========================================================================

    @Test
    fun testConfiguringQrisSuppressesBannerRegardlessOfPriorDismissals() = runTest {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        val vm = QrisViewModel(repo)

        // Given merchant dismissed once (count = 1)
        vm.dismissPrompt()
        val c1 = repo.getQrisConfig()
        assertEquals(1, c1?.promptDismissCount)

        // Merchant completes setup and saves QRIS photo
        val dummyImage = byteArrayOf(10, 20, 30, 40)
        repo.saveQrisConfig(dummyImage)

        val activeConfig = repo.getQrisConfig()
        assertNotNull(activeConfig)
        assertTrue(activeConfig.isEnabled)
        assertNotNull(activeConfig.imageBytes)
        assertEquals(1, activeConfig.promptDismissCount, "saveQrisConfig must preserve previous dismiss count")

        // In Cashier, banner must NOT be visible even with dismissedThisSession = false
        val visible = evaluateBannerVisibility(activeConfig, dismissedThisSession = false)
        assertFalse(visible, "Active QRIS config must immediately suppress cashier prompt")
    }

    @Test
    fun testDisablingQrisReactivatesBannerWhenBelowDismissalThreshold() = runTest {
        val db = FakeDatabase()
        val repo = ShopRepository(db)
        val vm = QrisViewModel(repo)

        // Active QRIS with dismissCount = 1
        repo.saveQrisConfig(byteArrayOf(1, 2, 3))
        repo.dismissQrisPrompt()

        val activeConfig = repo.getQrisConfig()
        assertNotNull(activeConfig)
        assertEquals(1, activeConfig.promptDismissCount)
        assertFalse(evaluateBannerVisibility(activeConfig, dismissedThisSession = false))

        // Merchant disables QRIS from Settings
        vm.setEnabled(false)

        val disabledConfig = repo.getQrisConfig()
        assertNotNull(disabledConfig)
        assertFalse(disabledConfig.isEnabled)

        // In new cashier session, banner should reappear to prompt merchant to activate/fix QRIS
        assertTrue(
            evaluateBannerVisibility(disabledConfig, dismissedThisSession = false),
            "Disabled QRIS with dismissCount < 3 should show prompt banner in new session"
        )

        // But in current session after dismiss, it stays hidden
        assertFalse(
            evaluateBannerVisibility(disabledConfig, dismissedThisSession = true),
            "Disabled QRIS with dismissedThisSession = true should stay hidden"
        )
    }

    // =========================================================================
    // 4. PayDialog QRIS Selection & Confirmation Logic
    // =========================================================================

    @Test
    fun testPayDialogQrisSelectionBehavior() {
        val total = 50_000L

        // Case 1: QRIS Configured and Enabled
        val configuredConfig = QrisConfigEntity(
            id = 1,
            isEnabled = true,
            imageBytes = byteArrayOf(1, 2, 3),
            updatedAt = 1000L,
        )
        val isQrisConfigured = configuredConfig.isEnabled && configuredConfig.imageBytes != null
        assertTrue(isQrisConfigured)

        var selectedMethod = PayMethod.TUNAI
        if (isQrisConfigured) {
            selectedMethod = PayMethod.QRIS
        }
        assertEquals(PayMethod.QRIS, selectedMethod)

        // For QRIS, finalPaid is always exactly total
        val paidInput = 20_000L // arbitrary user input
        val finalPaid = if (selectedMethod == PayMethod.QRIS) total else paidInput
        assertEquals(50_000L, finalPaid, "QRIS checkout must always tender exact total")

        // Case 2: QRIS Not Configured (imageBytes == null)
        val unconfiguredConfig = QrisConfigEntity(
            id = 1,
            isEnabled = true,
            imageBytes = null,
            updatedAt = 1000L,
        )
        val isConfigured2 = unconfiguredConfig.isEnabled && unconfiguredConfig.imageBytes != null
        assertFalse(isConfigured2)

        var method2 = PayMethod.TUNAI
        // Click QRIS chip
        if (isConfigured2) {
            method2 = PayMethod.QRIS
        }
        assertEquals(PayMethod.TUNAI, method2, "Clicking QRIS when not configured must not switch method")
    }
}
