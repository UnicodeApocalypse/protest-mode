package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import app.protestmode.R

object LocationSetting : Setting {
    override val id = SettingId.LOCATION
    override val titleRes = R.string.setting_location
    override val iconRes = R.drawable.ic_location

    @Suppress("DEPRECATION")
    override val observedUris = listOf(secureUri(Settings.Secure.LOCATION_MODE))

    override fun isAvailable(context: Context) = context.getSystemService(LocationManager::class.java) != null

    @Suppress("DEPRECATION")
    override fun read(probe: Probe): Reading {
        val context = probe.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Reading(context.getSystemService(LocationManager::class.java)?.isLocationEnabled.toSettingState())
        }
        val state =
            when (SettingsReader.secure(context.contentResolver, Settings.Secure.LOCATION_MODE)) {
                null -> SettingState.UNKNOWN
                Settings.Secure.LOCATION_MODE_OFF -> SettingState.OFF
                else -> SettingState.ON
            }
        return Reading(state)
    }

    override fun guideIntents(context: Context): List<Intent> = listOf(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}
