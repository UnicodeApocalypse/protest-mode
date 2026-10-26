package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import app.protestmode.R

// Both scans share one screen: "Scanning", with both switches (API 28-30), then "Location services", which
// links to each (API 31+). AOSP has a direct action for Wi-Fi scanning only (API 33+) and none for
// Bluetooth scanning, so both settings open the shared screen; before API 28 it is reached from Location.
private fun scanningScreens(): List<Intent> = listOf(
    Intent(HiddenActions.LOCATION_SCANNING),
    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
)

object WifiScanSetting : Setting {
    override val id = SettingId.WIFI_SCAN
    override val titleRes = R.string.setting_wifi_scan
    override val iconRes = R.drawable.ic_wifi_scan
    override val instructionRes = R.string.instruction_wifi_scan
    override val observedUris = listOf(globalUri(HiddenKeys.WIFI_SCAN_ALWAYS_ENABLED))

    override fun isAvailable(context: Context) = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

    override fun read(probe: Probe) = Reading(
        SettingsReader.toState(
            SettingsReader.global(probe.context.contentResolver, HiddenKeys.WIFI_SCAN_ALWAYS_ENABLED)
        )
    )

    override fun guideIntents(context: Context): List<Intent> = scanningScreens()
}

object BleScanSetting : Setting {
    override val id = SettingId.BLE_SCAN
    override val titleRes = R.string.setting_ble_scan
    override val iconRes = R.drawable.ic_ble_scan
    override val instructionRes = R.string.instruction_ble_scan
    override val observedUris = listOf(globalUri(HiddenKeys.BLE_SCAN_ALWAYS_ENABLED))

    override fun isAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    override fun read(probe: Probe) = Reading(
        SettingsReader.toState(
            SettingsReader.global(probe.context.contentResolver, HiddenKeys.BLE_SCAN_ALWAYS_ENABLED)
        )
    )

    override fun guideIntents(context: Context): List<Intent> = scanningScreens()
}
