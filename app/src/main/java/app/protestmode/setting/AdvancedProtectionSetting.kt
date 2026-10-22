package app.protestmode.setting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.security.advancedprotection.AdvancedProtectionManager
import app.protestmode.R

object AdvancedProtectionSetting : Setting {
    override val id = SettingId.ADVANCED_PROTECTION
    override val titleRes = R.string.setting_advanced_protection
    override val iconRes = R.drawable.ic_shield
    override val instructionRes = R.string.instruction_advanced_protection

    override fun isAvailable(context: Context) = manager(context) != null

    @SuppressLint("MissingPermission", "NewApi")
    override fun read(probe: Probe): Reading {
        val enabled =
            try {
                manager(probe.context)?.isAdvancedProtectionEnabled
            } catch (_: SecurityException) {
                null
            }
        return Reading(enabled.toSettingState())
    }

    // No public intent opens the Advanced Protection screen; it lives in the security settings.
    override fun guideIntents(context: Context): List<Intent> = listOf(Intent(Settings.ACTION_SECURITY_SETTINGS))

    @SuppressLint("NewApi")
    private fun manager(context: Context): AdvancedProtectionManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            context.getSystemService(AdvancedProtectionManager::class.java)
        } else {
            null
        }
}
