package app.protestmode.setting

import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NfcSettingTest {
    @Before
    fun enableNfcHardware() {
        setFeature(PackageManager.FEATURE_NFC, true)
    }

    @Test
    fun readsAdapterState() {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        shadowOf(adapter).setEnabled(true)
        assertEquals(SettingState.ON, NfcSetting.state(context))

        shadowOf(adapter).setEnabled(false)
        assertEquals(SettingState.OFF, NfcSetting.state(context))
    }

    @Test
    fun hiddenWithoutHardware() {
        assertTrue(NfcSetting.isAvailable(context))

        setFeature(PackageManager.FEATURE_NFC, false)
        assertFalse(NfcSetting.isAvailable(context))
    }

    @Test
    @Config(sdk = [29])
    fun usesPanelFromApi29() {
        assertEquals(
            listOf(Settings.Panel.ACTION_NFC, Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS),
            NfcSetting.guideActions()
        )
    }

    @Test
    @Config(sdk = [28])
    fun usesNfcScreenBeforeApi29() {
        assertEquals(listOf(Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS), NfcSetting.guideActions())
    }
}
