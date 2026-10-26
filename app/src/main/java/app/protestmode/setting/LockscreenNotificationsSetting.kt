package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.os.Build
import app.protestmode.R

object LockscreenNotificationsSetting : Setting {
    override val id = SettingId.LOCKSCREEN_NOTIFICATIONS
    override val titleRes = R.string.setting_lockscreen_notifications
    override val iconRes = R.drawable.ic_notifications
    override val instructionRes
        get() = when {
            VendorSkin.matches() -> R.string.instruction_lockscreen_notifications_vendor
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N -> R.string.instruction_lockscreen_notifications_v6
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> R.string.instruction_lockscreen_notifications_v7
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> R.string.instruction_lockscreen_notifications_v9
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> R.string.instruction_lockscreen_notifications_v10
            Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA -> R.string.instruction_lockscreen_notifications
            else -> R.string.instruction_lockscreen_notifications_v16
        }
    override val onLabelRes = R.string.state_content_visible
    override val offLabelRes = R.string.state_content_hidden
    override val observedUris =
        listOf(
            secureUri(HiddenKeys.LOCK_SCREEN_SHOW_NOTIFICATIONS),
            secureUri(HiddenKeys.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS)
        )

    override fun isAvailable(context: Context) = true

    // Hiding silent notifications only (lock_screen_show_silent_notifications) still shows the others.
    override fun read(probe: Probe): Reading {
        val resolver = probe.context.contentResolver
        val state =
            stateFor(
                show = SettingsReader.secure(resolver, HiddenKeys.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                allowPrivate = SettingsReader.secure(resolver, HiddenKeys.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS),
                screenLock = probe.screenLock
            )
        val detail = if (state ==
            SettingState.ON
        ) {
            probe.context.getString(R.string.detail_lockscreen_notifications)
        } else {
            null
        }
        return Reading(state, detail)
    }

    /** Hiding sensitive content only protects anything behind a secure lock screen. */
    internal fun stateFor(show: Int?, allowPrivate: Int?, screenLock: SettingState): SettingState = when {
        show == 0 -> SettingState.OFF
        allowPrivate == 0 && screenLock == SettingState.ON -> SettingState.OFF
        show == 1 && (allowPrivate == 1 || screenLock == SettingState.OFF) -> SettingState.ON
        else -> SettingState.UNKNOWN
    }

    // The Lock screen page (API 31+) lacks the "Sensitive notifications" switch, and OxygenOS asks for the
    // screen lock before opening it. AOSP and every documented skin keep these options under Notifications;
    // API 36 adds a dedicated page.
    override fun guideIntents(context: Context): List<Intent> = listOf(
        Intent(HiddenActions.LOCK_SCREEN_NOTIFICATIONS),
        Intent(HiddenActions.NOTIFICATION)
    )
}
