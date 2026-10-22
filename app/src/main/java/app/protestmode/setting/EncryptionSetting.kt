package app.protestmode.setting

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.protestmode.R

object EncryptionSetting : Setting {
    override val id = SettingId.ENCRYPTION
    override val titleRes = R.string.setting_encryption
    override val iconRes = R.drawable.ic_key
    override val instructionRes = R.string.instruction_encryption
    override val onLabelRes = R.string.state_encrypted
    override val offLabelRes = R.string.state_not_encrypted

    override fun isAvailable(context: Context) = context.getSystemService(DevicePolicyManager::class.java) != null

    override fun read(probe: Probe): Reading {
        val status =
            probe.context.getSystemService(DevicePolicyManager::class.java)?.storageEncryptionStatus
                ?: return Reading(SettingState.UNKNOWN)
        val withoutCode =
            status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY || probe.screenLock == SettingState.OFF
        val detail = if (withoutCode) probe.context.getString(R.string.detail_encryption_default_key) else null
        return Reading(stateFor(status, probe.screenLock), detail)
    }

    // Without a secure lock screen the keys do not depend on any user secret, so the data is not protected.
    // ACTIVE_DEFAULT_KEY: same situation reported by full-disk encryption devices.
    internal fun stateFor(status: Int, screenLock: SettingState): SettingState = when (status) {
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
        -> screenLock
        DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE,
        DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED,
        DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        -> SettingState.OFF
        else -> SettingState.UNKNOWN
    }

    override fun guideIntents(context: Context): List<Intent> = listOf(Intent(Settings.ACTION_SECURITY_SETTINGS))
}
