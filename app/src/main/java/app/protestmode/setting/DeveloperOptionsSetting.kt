package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.protestmode.R

object DeveloperOptionsSetting : Setting {
    override val id = SettingId.DEVELOPER_OPTIONS
    override val titleRes = R.string.setting_developer_options
    override val iconRes = R.drawable.ic_code
    override val instructionRes = R.string.instruction_developer_options
    override val observedUris =
        listOf(
            globalUri(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            globalUri(Settings.Global.ADB_ENABLED),
            globalUri(HiddenKeys.ADB_WIFI_ENABLED)
        )

    override fun isAvailable(context: Context) = true

    override fun read(probe: Probe): Reading {
        val resolver = probe.context.contentResolver
        val developer = SettingsReader.global(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)
        val adb = SettingsReader.global(resolver, Settings.Global.ADB_ENABLED)
        // Wireless debugging (API 30+) uses a hidden Settings.Global key.
        val adbWifi = SettingsReader.global(resolver, HiddenKeys.ADB_WIFI_ENABLED)
        val detail =
            when {
                adb == 1 -> R.string.detail_usb_debugging
                adbWifi == 1 -> R.string.detail_wireless_debugging
                else -> null
            }
        return Reading(stateFor(developer, adb, adbWifi), detail?.let(probe.context::getString))
    }

    internal fun stateFor(developer: Int?, adb: Int?, adbWifi: Int?): SettingState = when {
        developer == 1 || adb == 1 || adbWifi == 1 -> SettingState.ON
        developer == null && adb == null -> SettingState.UNKNOWN
        else -> SettingState.OFF
    }

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
}
