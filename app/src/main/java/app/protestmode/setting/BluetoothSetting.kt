package app.protestmode.setting

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import app.protestmode.R

object BluetoothSetting : Setting {
    override val id = SettingId.BLUETOOTH
    override val titleRes = R.string.setting_bluetooth
    override val iconRes = R.drawable.ic_bluetooth
    override val observedUris = listOf(globalUri(Settings.Global.BLUETOOTH_ON))

    override fun isAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && adapter(context) != null

    // BluetoothAdapter.getState() requires the legacy BLUETOOTH permission up to API 30 and no permission afterwards.
    @SuppressLint("MissingPermission")
    override fun read(probe: Probe): Reading {
        val adapter = adapter(probe.context)
        if (adapter != null) {
            try {
                val state =
                    when (adapter.state) {
                        BluetoothAdapter.STATE_ON, BluetoothAdapter.STATE_TURNING_ON -> SettingState.ON
                        BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> SettingState.OFF
                        else -> SettingState.UNKNOWN
                    }
                return Reading(state)
            } catch (_: SecurityException) {
                // Some vendor builds enforce BLUETOOTH_CONNECT; the persisted global setting is used instead.
            }
        }
        val persisted =
            when (SettingsReader.global(probe.context.contentResolver, Settings.Global.BLUETOOTH_ON)) {
                0 -> SettingState.OFF
                1 -> SettingState.ON
                else -> SettingState.UNKNOWN
            }
        return Reading(persisted)
    }

    // BluetoothAdapter.disable() requires the dangerous BLUETOOTH_CONNECT permission on API 31-32
    // and is a no-op for regular apps from API 33, so it is only attempted up to API 30.
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun tryProtect(context: Context): Boolean {
        if (!canDisableAutomatically()) return false
        val adapter = adapter(context) ?: return false
        return try {
            adapter.disable()
        } catch (_: SecurityException) {
            false
        }
    }

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))

    internal fun canDisableAutomatically() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.R

    private fun adapter(context: Context): BluetoothAdapter? =
        context.applicationContext.getSystemService(BluetoothManager::class.java)?.adapter
}
