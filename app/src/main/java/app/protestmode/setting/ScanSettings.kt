package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import app.protestmode.R

// AOSP groups both scans on one screen: Location > menu > Scanning (API 23-25), Location > Scanning
// (API 26-27), "Scanning" opened directly (API 28-30), then "Location services", which links to each
// (API 31+). It has a direct action for Wi-Fi scanning only (API 33+) and none for Bluetooth scanning.
// Vendor skins get the shared screen.
internal object ScanScreens {
    fun shared(): List<Intent> = listOf(
        Intent(HiddenActions.LOCATION_SCANNING),
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    )

    @StringRes
    fun instruction(@StringRes menu: Int, @StringRes entry: Int, @StringRes direct: Int, @StringRes vendor: Int): Int =
        when {
            VendorSkin.matches() -> vendor
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O -> menu
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> entry
            else -> direct
        }
}

object WifiScanSetting : Setting {
    override val id = SettingId.WIFI_SCAN
    override val titleRes = R.string.setting_wifi_scan
    override val iconRes = R.drawable.ic_wifi_scan
    override val instructionRes
        get() = ScanScreens.instruction(
            R.string.instruction_wifi_scan_menu,
            R.string.instruction_wifi_scan_entry,
            R.string.instruction_wifi_scan,
            R.string.instruction_wifi_scan_vendor
        )
    override val observedUris = listOf(globalUri(HiddenKeys.WIFI_SCAN_ALWAYS_ENABLED))

    override fun isAvailable(context: Context) = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

    override fun read(probe: Probe) = Reading(
        SettingsReader.toState(
            SettingsReader.global(probe.context.contentResolver, HiddenKeys.WIFI_SCAN_ALWAYS_ENABLED)
        )
    )

    // The shared screen only saves a trip while Bluetooth scanning is on too.
    override fun guideIntents(context: Context): List<Intent> {
        val bleScanOn = SettingsReader.global(context.contentResolver, HiddenKeys.BLE_SCAN_ALWAYS_ENABLED) == 1
        val direct = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !VendorSkin.matches() && !bleScanOn
        return if (direct) listOf(Intent(HiddenActions.WIFI_SCANNING)) + ScanScreens.shared() else ScanScreens.shared()
    }
}

object BleScanSetting : Setting {
    override val id = SettingId.BLE_SCAN
    override val titleRes = R.string.setting_ble_scan
    override val iconRes = R.drawable.ic_ble_scan
    override val instructionRes
        get() = ScanScreens.instruction(
            R.string.instruction_ble_scan_menu,
            R.string.instruction_ble_scan_entry,
            R.string.instruction_ble_scan,
            R.string.instruction_ble_scan_vendor
        )
    override val observedUris = listOf(globalUri(HiddenKeys.BLE_SCAN_ALWAYS_ENABLED))

    override fun isAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    override fun read(probe: Probe) = Reading(
        SettingsReader.toState(
            SettingsReader.global(probe.context.contentResolver, HiddenKeys.BLE_SCAN_ALWAYS_ENABLED)
        )
    )

    override fun guideIntents(context: Context): List<Intent> = ScanScreens.shared()
}
