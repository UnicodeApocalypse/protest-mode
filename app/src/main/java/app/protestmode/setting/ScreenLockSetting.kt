package app.protestmode.setting

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.protestmode.R

object ScreenLockSetting : Setting {
    override val id = SettingId.SCREEN_LOCK
    override val titleRes = R.string.setting_screen_lock
    override val iconRes = R.drawable.ic_lock
    override val instructionRes = R.string.instruction_screen_lock
    override val onLabelRes = R.string.state_secure
    override val offLabelRes = R.string.state_not_secure

    override fun isAvailable(context: Context) = context.getSystemService(KeyguardManager::class.java) != null

    override fun read(probe: Probe) = Reading(probe.screenLock)

    internal fun readScreenLock(context: Context) =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure.toSettingState()

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD), Intent(Settings.ACTION_SECURITY_SETTINGS))
}
