package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.protestmode.R

object AirplaneSetting : Setting {
    override val id = SettingId.AIRPLANE
    override val titleRes = R.string.setting_airplane
    override val iconRes = R.drawable.ic_airplane
    override val observedUris = listOf(globalUri(Settings.Global.AIRPLANE_MODE_ON))

    override fun isAvailable(context: Context) = true

    override fun read(probe: Probe): Reading {
        val detail =
            when (probe.airplane) {
                SettingState.ON -> R.string.detail_airplane_on
                SettingState.OFF -> R.string.detail_airplane_off
                SettingState.UNKNOWN -> null
            }
        return Reading(probe.airplane, detail?.let(probe.context::getString))
    }

    internal fun readAirplane(context: Context) =
        SettingsReader.toState(SettingsReader.global(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON))

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
}
