package app.protestmode

import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.protestmode.setting.LocationSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun waitForSettings() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText(string(R.string.category_tracking))).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    @Test
    fun everyRowStatesItsStatusInText() {
        val title = string(LocationSetting.titleRes)
        val stateTexts = listOf(
            R.string.setting_state_on,
            R.string.setting_state_off,
            R.string.setting_state_unknown
        ).map {
            string(it)
        }
        val node = compose.onNodeWithContentDescription(title, substring = true).fetchSemanticsNode()
        val description = node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.joinToString()
        assertTrue(description, stateTexts.any { it in description })
    }

    @Test
    fun showsBannerAndSecuredLabels() {
        listOf(
            R.string.label_device_access,
            R.string.label_location_tracking,
            R.string.label_fingerprinting,
            R.string.label_activity_history
        )
            .forEach { compose.onNodeWithText(string(it)).assertIsDisplayed() }
        val banners = listOf(R.string.status_exposed, R.string.status_partial, R.string.status_discreet).map {
            string(it)
        }
        assertEquals(1, banners.count { compose.onAllNodes(hasText(it)).fetchSemanticsNodes().isNotEmpty() })
    }

    @Test
    fun tappingALabelFiltersTheList() {
        compose.onNodeWithText(string(R.string.label_device_access)).performClick()
        assertTrue(compose.onAllNodes(hasText(string(R.string.setting_wifi))).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(string(R.string.label_device_access)).performClick()
        assertTrue(compose.onAllNodes(hasText(string(R.string.setting_wifi))).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun everyRowAnnouncesItsRiskLevel() {
        val risks = listOf(R.string.risk_critical_a11y, R.string.risk_high_a11y, R.string.risk_moderate_a11y).map {
            string(it)
        }
        val node = compose.onNodeWithContentDescription(
            string(LocationSetting.titleRes),
            substring = true
        ).fetchSemanticsNode()
        val description = node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.joinToString()
        assertTrue(description, risks.any { it in description })
    }

    @Test
    fun rowsRefreshWhenASettingChangesWithoutLeavingTheApp() {
        val key = "ble_scan_always_enabled"
        val original = shell("settings get global $key").trim()
        val title = string(R.string.setting_ble_scan)
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription(title, substring = true))
        fun description() = compose
            .onNodeWithContentDescription(title, substring = true)
            .fetchSemanticsNode()
            .config
            .getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
            .joinToString()
        val before = description()
        try {
            shell("settings put global $key ${if (original == "1") 0 else 1}")
            compose.waitUntil(timeoutMillis = 5_000) { description() != before }
        } finally {
            shell("settings put global $key ${original.takeIf { it == "0" || it == "1" } ?: 0}")
        }
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    @Test
    fun touchTargetsAreAtLeast48dp() {
        listOf(
            compose.onNodeWithContentDescription(string(LocationSetting.titleRes), substring = true),
            compose.onNodeWithText(string(R.string.label_device_access))
        ).forEach { node ->
            val touchHeight = with(compose.density) { node.fetchSemanticsNode().touchBoundsInRoot.height.toDp() }
            assertTrue("touch height $touchHeight", touchHeight >= 47.5.dp)
        }
    }
}
