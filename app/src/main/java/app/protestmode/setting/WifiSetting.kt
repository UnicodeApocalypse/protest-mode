package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import app.protestmode.R

object WifiSetting : Setting {
    override val id = SettingId.WIFI
    override val titleRes = R.string.setting_wifi
    override val iconRes = R.drawable.ic_wifi
    override val observedUris = listOf(globalUri(HiddenKeys.WIFI_ON))

    override fun isAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI) && wifiManager(context) != null

    override fun read(probe: Probe): Reading {
        val manager = wifiManager(probe.context) ?: return Reading(SettingState.UNKNOWN)
        val state =
            when (manager.wifiState) {
                WifiManager.WIFI_STATE_ENABLED, WifiManager.WIFI_STATE_ENABLING -> SettingState.ON
                WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> SettingState.OFF
                else -> SettingState.UNKNOWN
            }
        return Reading(state)
    }

    // WifiManager.setWifiEnabled() always returns false for apps targeting API 29 or higher.
    override fun guideIntents(context: Context): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Intent(Settings.Panel.ACTION_WIFI))
        add(Intent(Settings.ACTION_WIFI_SETTINGS))
        add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    private fun wifiManager(context: Context) = context.applicationContext.getSystemService(WifiManager::class.java)
}
