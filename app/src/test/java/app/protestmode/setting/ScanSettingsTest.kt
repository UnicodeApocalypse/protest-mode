package app.protestmode.setting

import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

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
    fun detectsVendorSkins() {
        assertTrue(VendorSkin.matches("samsung", "samsung"))
        assertTrue(VendorSkin.matches("Xiaomi", "Redmi"))
        assertTrue(VendorSkin.matches("TECNO MOBILE LIMITED", "TECNO"))
        assertFalse(VendorSkin.matches("Google", "google"))
        assertFalse(VendorSkin.matches("motorola", "motorola"))
    }

    @Test
    @Config(sdk = [23])
    fun givesTheMenuPathBeforeAndroid8() {
        assertEquals(R.string.instruction_wifi_scan_menu, WifiScanSetting.instructionRes)
        assertEquals(R.string.instruction_ble_scan_menu, BleScanSetting.instructionRes)
    }

    @Test
    @Config(sdk = [26])
    fun givesTheScanningEntryOnAndroid8() {
        assertEquals(R.string.instruction_wifi_scan_entry, WifiScanSetting.instructionRes)
    }

    @Test
    @Config(sdk = [28])
    fun namesTheSwitchOnlyWhenTheScreenOpensDirectly() {
        assertEquals(R.string.instruction_wifi_scan, WifiScanSetting.instructionRes)
        assertEquals(R.string.instruction_ble_scan, BleScanSetting.instructionRes)
    }

    @Test
    fun givesNoMenuPathOnVendorSkins() {
        ShadowBuild.setManufacturer("samsung")
        assertEquals(R.string.instruction_wifi_scan_vendor, WifiScanSetting.instructionRes)
        assertEquals(R.string.instruction_ble_scan_vendor, BleScanSetting.instructionRes)
    }

    @Test
    @Config(sdk = [33])
    fun opensTheWifiScanningPageWhenBluetoothScanningIsOff() {
        Settings.Global.putInt(context.contentResolver, "ble_scan_always_enabled", 0)
        assertEquals("android.settings.WIFI_SCANNING_SETTINGS", WifiScanSetting.guideActions().first())

        Settings.Global.putInt(context.contentResolver, "ble_scan_always_enabled", 1)
        assertEquals(BleScanSetting.guideActions(), WifiScanSetting.guideActions())
    }

    @Test
    @Config(sdk = [33])
    fun opensTheSharedScreenOnVendorSkins() {
        ShadowBuild.setManufacturer("OPPO")
        Settings.Global.putInt(context.contentResolver, "ble_scan_always_enabled", 0)
        assertEquals(BleScanSetting.guideActions(), WifiScanSetting.guideActions())
    }

    @Test
    fun fallsBackToLocationScreen() {
        assertTrue(Settings.ACTION_LOCATION_SOURCE_SETTINGS in WifiScanSetting.guideActions())
        assertEquals(Settings.ACTION_LOCATION_SOURCE_SETTINGS, BleScanSetting.guideActions().last())
    }
}
