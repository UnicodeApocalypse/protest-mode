package app.protestmode.setting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingIdTest {
    @Test
    fun `every setting carries at least one label`() {
        SettingId.entries.forEach { assertTrue("$it", it.labels.isNotEmpty()) }
    }

    @Test
    fun `every label covers at least one critical or important setting`() {
        ExposureLabel.entries.forEach { label ->
            assertTrue("$label", SettingId.entries.any { label in it.labels && it.risk != RiskLevel.MODERATE })
        }
    }

    @Test
    fun `categories are contiguous in the display order`() {
        val categories = SettingId.entries.map { it.category }
        val blocks = categories.zipWithNext().count { (a, b) -> a != b } + 1

        assertEquals(categories.distinct().size, blocks)
        assertEquals(
            listOf(SettingCategory.TRACKING, SettingCategory.SECURITY, SettingCategory.ANONYMITY),
            categories.distinct()
        )
    }

    @Test
    fun `risk levels are sorted within each category`() {
        SettingCategory.entries.forEach { category ->
            val risks = SettingId.entries.filter { it.category == category }.map { it.risk }
            assertEquals("$category", risks.sorted(), risks)
        }
    }

    @Test
    fun `levels requested for the protest context`() {
        assertEquals(RiskLevel.CRITICAL, SettingId.AIRPLANE.risk)
        assertEquals(RiskLevel.MODERATE, SettingId.BIOMETRICS.risk)
        assertEquals(RiskLevel.CRITICAL, SettingId.DEVELOPER_OPTIONS.risk)
    }

    @Test
    fun `protective target depends on the setting`() {
        listOf(
            SettingId.AIRPLANE,
            SettingId.SCREEN_LOCK,
            SettingId.ENCRYPTION,
            SettingId.SECURITY_PATCH,
            SettingId.ADVANCED_PROTECTION
        )
            .forEach { assertTrue("$it", it.desiredEnabled) }
        SettingId.entries
            .filter { it.category == SettingCategory.TRACKING && it != SettingId.AIRPLANE }
            .forEach { assertTrue("$it", !it.desiredEnabled) }
    }
}
