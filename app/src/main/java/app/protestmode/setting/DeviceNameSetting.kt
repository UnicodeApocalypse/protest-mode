package app.protestmode.setting

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import app.protestmode.R

object DeviceNameSetting : Setting {
    private val PERSONAL_MARKERS = Regex("""(['’]s\b|\bde\b|\bd['’]|\bdi\b|\bvon\b|\bdel\b)""", RegexOption.IGNORE_CASE)

    override val id = SettingId.DEVICE_NAME
    override val titleRes = R.string.setting_device_name
    override val iconRes = R.drawable.ic_smartphone
    override val instructionRes = R.string.instruction_device_name
    override val onLabelRes = R.string.state_personal
    override val offLabelRes = R.string.state_generic
    override val unknownLabelRes = R.string.state_to_check
    override val observedUris = listOf(globalUri(Settings.Global.DEVICE_NAME))

    override fun isAvailable(context: Context) = true

    override fun read(probe: Probe): Reading {
        val name = name(probe.context)
        return Reading(
            state = stateFor(name, Build.MANUFACTURER, Build.MODEL),
            detail = name?.let { probe.context.getString(R.string.detail_device_name, it) },
            confirmationKey = name
        )
    }

    internal fun stateFor(name: String?, manufacturer: String, model: String): SettingState {
        val trimmed = name?.trim()
        if (trimmed.isNullOrEmpty()) return SettingState.UNKNOWN
        val generic = setOf(model, "$manufacturer $model").map { it.trim().lowercase() }
        return when {
            trimmed.lowercase() in generic -> SettingState.OFF
            PERSONAL_MARKERS.containsMatchIn(trimmed) -> SettingState.ON
            else -> SettingState.UNKNOWN
        }
    }

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(HiddenActions.DEVICE_NAME), Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))

    // Settings.Global.DEVICE_NAME appears in API 25; the Bluetooth name is read before that.
    @SuppressLint("MissingPermission")
    private fun name(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)?.let { return it }
        }
        return try {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.name
        } catch (_: SecurityException) {
            null
        }
    }
}
