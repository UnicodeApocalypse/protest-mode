package app.protestmode.setting

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import app.protestmode.R

object VoiceAssistantSetting : Setting {
    /** Must stay in sync with the <queries> block of the manifest. */
    internal val ASSISTANT_PACKAGES =
        listOf(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.bard",
            "com.samsung.android.bixby.agent"
        )

    override val id = SettingId.VOICE_ASSISTANT
    override val titleRes = R.string.setting_voice_assistant
    override val iconRes = R.drawable.ic_mic
    override val instructionRes = R.string.instruction_voice_assistant
    override val onLabelRes = R.string.state_mic_allowed
    override val offLabelRes = R.string.state_mic_blocked

    override fun isAvailable(context: Context) = installed(context).isNotEmpty()

    override fun read(probe: Probe): Reading {
        val context = probe.context
        val installed = installed(context)
        if (installed.isEmpty()) return Reading(SettingState.UNKNOWN)
        val listening = listening(context, installed)
        val names = listening.map { context.packageManager.getApplicationLabel(it).toString() }
        val detail = if (names.isEmpty()) {
            null
        } else {
            context.getString(
                R.string.detail_voice_assistant,
                names.joinToString()
            )
        }
        return Reading(listening.isNotEmpty().toSettingState(), detail)
    }

    override fun guideIntents(context: Context): List<Intent> {
        val installed = installed(context)
        val target = listening(context, installed).firstOrNull() ?: installed.firstOrNull()
        return buildList {
            if (target != null) {
                add(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", target.packageName, null)
                    )
                )
            }
            add(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun installed(context: Context): List<ApplicationInfo> = ASSISTANT_PACKAGES.mapNotNull { pkg ->
        try {
            context.packageManager.getApplicationInfo(pkg, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun listening(context: Context, installed: List<ApplicationInfo>): List<ApplicationInfo> =
        installed.filter {
            it.enabled &&
                context.packageManager.checkPermission(Manifest.permission.RECORD_AUDIO, it.packageName) ==
                PackageManager.PERMISSION_GRANTED
        }
}
