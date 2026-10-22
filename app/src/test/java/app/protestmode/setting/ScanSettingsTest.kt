package app.protestmode.setting

import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanSettingsTest {
    @Test
    fun readsWifiScanSetting() {
        assertEquals(SettingState.UNKNOWN, WifiScanSetting.state(context))

        Settings.Global.putInt(context.contentResolver, "wifi_scan_always_enabled", 1)
        assertEquals(SettingState.ON, WifiScanSetting.state(context))

        Settings.Global.putInt(context.contentResolver, "wifi_scan_always_enabled", 0)
        assertEquals(SettingState.OFF, WifiScanSetting.state(context))
    }

    @Test
    fun readsBleScanSetting() {
        assertEquals(SettingState.UNKNOWN, BleScanSetting.state(context))

        Settings.Global.putInt(context.contentResolver, "ble_scan_always_enabled", 1)
        assertEquals(SettingState.ON, BleScanSetting.state(context))

        Settings.Global.putInt(context.contentResolver, "ble_scan_always_enabled", 0)
        assertEquals(SettingState.OFF, BleScanSetting.state(context))
    }

    @Test
    fun availabilityFollowsHardware() {
        setFeature(PackageManager.FEATURE_WIFI, true)
        setFeature(PackageManager.FEATURE_BLUETOOTH_LE, false)
        assertTrue(WifiScanSetting.isAvailable(context))
        assertFalse(BleScanSetting.isAvailable(context))
    }

    @Test
    fun fallsBackToLocationScreen() {
        assertTrue(Settings.ACTION_LOCATION_SOURCE_SETTINGS in WifiScanSetting.guideActions())
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, BleScanSetting.guideActions().last())
    }
}
