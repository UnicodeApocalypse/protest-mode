package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import app.protestmode.R

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

    override fun guideIntents(context: Context): List<Intent> = listOf(
        Intent(HiddenActions.WIFI_SCANNING),
        Intent(HiddenActions.LOCATION_SCANNING),
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        Intent(Settings.ACTION_WIFI_IP_SETTINGS)
    )
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

    override fun guideIntents(context: Context): List<Intent> = listOf(
        Intent(HiddenActions.BLUETOOTH_SCANNING),
        Intent(HiddenActions.LOCATION_SCANNING),
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    )
}
