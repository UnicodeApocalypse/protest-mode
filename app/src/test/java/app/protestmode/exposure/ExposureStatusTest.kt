package app.protestmode.exposure

import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.RiskLevel
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingId.BLE_SCAN
import app.protestmode.setting.SettingId.LOCATION
import app.protestmode.setting.SettingId.NFC
import app.protestmode.setting.SettingId.WIFI
import app.protestmode.setting.SettingState
import app.protestmode.setting.SettingState.OFF
import app.protestmode.setting.SettingState.ON
import app.protestmode.setting.SettingState.UNKNOWN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureStatusTest {
    private val allGood = mapOf(LOCATION to OFF, WIFI to OFF, BLE_SCAN to OFF, NFC to OFF)

    private fun evaluate(vararg overrides: Pair<SettingId, SettingState>, confirmed: Set<SettingId> = emptySet()) =
        Exposure.evaluate(allGood + overrides, confirmed)

    @Test
    fun `risk levels used by these tests`() {
        assertEquals(RiskLevel.CRITICAL, LOCATION.risk)
        assertEquals(RiskLevel.HIGH, WIFI.risk)
        assertEquals(RiskLevel.MODERATE, BLE_SCAN.risk)
        assertEquals(RiskLevel.MODERATE, NFC.risk)
    }

    @Test
    fun `everything compliant is discreet`() {
        assertEquals(ExposureStatus.DISCREET, evaluate().status)
    }

    @Test
    fun `one critical setting is enough to be exposed`() {
        assertEquals(ExposureStatus.EXPOSED, evaluate(LOCATION to ON).status)
        assertEquals(ExposureStatus.EXPOSED, evaluate(LOCATION to ON, WIFI to ON).status)
    }

    @Test
    fun `important settings leave the user partially protected`() {
        assertEquals(ExposureStatus.PARTIAL, evaluate(WIFI to ON).status)
    }

    @Test
    fun `advised settings never prevent the discreet state`() {
        val exposure = evaluate(BLE_SCAN to ON, NFC to UNKNOWN)

        assertEquals(ExposureStatus.DISCREET, exposure.status)
        assertEquals(2, exposure.remainingAdvice)
    }

    @Test
    fun `unknown state counts as unprotected`() {
        assertEquals(ExposureStatus.PARTIAL, evaluate(WIFI to UNKNOWN).status)
    }

    @Test
    fun `confirmed unknown state counts as protected`() {
        assertEquals(ExposureStatus.DISCREET, evaluate(WIFI to UNKNOWN, confirmed = setOf(WIFI)).status)
    }

    @Test
    fun `confirmation does not hide a readable setting that is on`() {
        assertEquals(ExposureStatus.PARTIAL, evaluate(WIFI to ON, confirmed = setOf(WIFI)).status)
    }

    @Test
    fun `nothing to check is discreet`() {
        val exposure = Exposure.evaluate(emptyMap())

        assertEquals(ExposureStatus.DISCREET, exposure.status)
        assertEquals(0, exposure.remainingAdvice)
    }

    @Test
    fun `every label is secured when everything is compliant`() {
        assertEquals(ExposureLabel.entries.toSet(), evaluate().securedLabels)
    }

    @Test
    fun `an unprotected important setting unsecures each of its labels`() {
        val labels = evaluate(WIFI to ON).securedLabels

        WIFI.labels.forEach { assertFalse("$it", it in labels) }
        assertTrue(ExposureLabel.DEVICE_ACCESS in labels)
    }

    @Test
    fun `advised settings never unsecure a label`() {
        assertEquals(ExposureLabel.entries.toSet(), evaluate(BLE_SCAN to ON, NFC to ON).securedLabels)
    }
}
