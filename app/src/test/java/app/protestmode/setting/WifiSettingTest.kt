package app.protestmode.setting

import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class WifiSettingTest {
    private val wifi get() = context.getSystemService(WifiManager::class.java)

    @Suppress("DEPRECATION")
    @Test
    fun readsWifiState() {
        wifi.setWifiEnabled(true)
        assertEquals(SettingState.ON, WifiSetting.state(context))

        wifi.setWifiEnabled(false)
        assertEquals(SettingState.OFF, WifiSetting.state(context))
    }

    @Test
    fun availabilityFollowsHardware() {
        setFeature(PackageManager.FEATURE_WIFI, true)
        assertTrue(WifiSetting.isAvailable(context))

        setFeature(PackageManager.FEATURE_WIFI, false)
        assertFalse(WifiSetting.isAvailable(context))
    }

    @Test
    fun neverDisablesAutomatically() {
        assertFalse(WifiSetting.tryProtect(context))
    }

    @Test
    @Config(sdk = [29])
    fun usesPanelFromApi29() {
        assertEquals(
            listOf(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS),
            WifiSetting.guideActions()
        )
    }

    @Test
    @Config(sdk = [28])
    fun usesClassicScreenBeforeApi29() {
        assertEquals(
            listOf(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS),
            WifiSetting.guideActions()
        )
    }
}
