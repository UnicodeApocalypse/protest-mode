package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import app.protestmode.R

object UwbSetting : Setting {
    private const val FEATURE_UWB = "android.hardware.uwb"

    override val id = SettingId.UWB
    override val titleRes = R.string.setting_uwb
    override val iconRes = R.drawable.ic_uwb
    override val instructionRes = R.string.instruction_uwb

    override fun isAvailable(context: Context) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.packageManager.hasSystemFeature(FEATURE_UWB)

    // UwbManager is a system API: third-party apps cannot read the UWB toggle.
    override fun read(probe: Probe) = Reading(SettingState.UNKNOWN)

    override fun guideIntents(context: Context): List<Intent> = listOf(
        Intent(HiddenActions.UWB),
        Intent(HiddenActions.ADVANCED_CONNECTED_DEVICE),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    )
}
