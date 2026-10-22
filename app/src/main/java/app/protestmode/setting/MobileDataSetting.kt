package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import app.protestmode.R

object MobileDataSetting : Setting {
    override val id = SettingId.MOBILE_DATA
    override val titleRes = R.string.setting_mobile_data
    override val iconRes = R.drawable.ic_mobile_data
    override val observedUris = listOf(globalUri(HiddenKeys.MOBILE_DATA), globalUri(Settings.Global.AIRPLANE_MODE_ON))

    override fun isAvailable(context: Context): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return false
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return false
        return telephony.phoneType != TelephonyManager.PHONE_TYPE_NONE
    }

    /**
     * On dual-SIM phones the toggle only describes the default data SIM, while the other SIM may still
     * carry data (automatic data switch, data during calls). A cellular network providing Internet,
     * whatever its SIM, therefore also counts as enabled.
     */
    override fun read(probe: Probe): Reading {
        val context = probe.context
        // Airplane mode turns every modem off, so the per-SIM toggle no longer matters.
        if (probe.airplane == SettingState.ON) {
            return Reading(SettingState.OFF, context.getString(R.string.detail_mobile_data_airplane))
        }
        val active = cellularDataActive(context)
        val detail = if (active) context.getString(R.string.detail_cellular_data_active) else null
        return Reading(stateFor(defaultSimToggle(context), active), detail)
    }

    internal fun stateFor(defaultSimToggle: SettingState, cellularDataActive: Boolean): SettingState =
        if (cellularDataActive) SettingState.ON else defaultSimToggle

    private fun defaultSimToggle(context: Context): SettingState {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val telephony = context.getSystemService(TelephonyManager::class.java)
                if (telephony != null) return telephony.isDataEnabled.toSettingState()
            } catch (_: SecurityException) {
                // API 26-27 may require READ_PHONE_STATE; the global setting is read instead.
            }
        }
        return SettingsReader.toState(SettingsReader.global(context.contentResolver, HiddenKeys.MOBILE_DATA))
    }

    // ConnectivityManager.getAllNetworks() is deprecated in favour of callbacks, but a one-shot read
    // is enough here and only needs ACCESS_NETWORK_STATE.
    @Suppress("DEPRECATION")
    internal fun cellularDataActive(context: Context): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return try {
            connectivity.allNetworks.any { network ->
                val capabilities = connectivity.getNetworkCapabilities(network) ?: return@any false
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        } catch (_: SecurityException) {
            false
        }
    }

    override fun guideIntents(context: Context): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
        add(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
        add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }
}
