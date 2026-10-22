package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.provider.Settings
import app.protestmode.R

/**
 * ON means that the phone stays unlocked longer than [MAX_LOCK_DELAY_MS] once left alone. The real
 * delay is the screen timeout plus the "lock after screen timeout" delay that follows it, and the
 * phone never locks without a secure screen lock. The power button instant lock and Smart Lock live
 * in the lock settings database, which apps cannot read.
 */
object LockDelaySetting : Setting {
    internal const val MAX_LOCK_DELAY_MS = 60_000L

    /** AOSP value used when the lock delay has never been changed. */
    internal const val DEFAULT_LOCK_AFTER_MS = 5_000L

    override val id = SettingId.LOCK_DELAY
    override val titleRes = R.string.setting_lock_delay
    override val iconRes = R.drawable.ic_timer
    override val instructionRes = R.string.instruction_lock_delay
    override val onLabelRes = R.string.state_too_long
    override val offLabelRes = R.string.state_short
    override val observedUris =
        listOf(systemUri(Settings.System.SCREEN_OFF_TIMEOUT), secureUri(HiddenKeys.LOCK_SCREEN_LOCK_AFTER_TIMEOUT))

    override fun isAvailable(context: Context) = true

    override fun read(probe: Probe): Reading {
        val context = probe.context
        val screenOff = screenOff(context)
        val lockAfter = lockAfter(context)
        return Reading(
            stateFor(screenOff, lockAfter, probe.screenLock),
            detail(context, screenOff, lockAfter, probe.screenLock)
        )
    }

    internal fun stateFor(screenOffMs: Int?, lockAfterMs: Long?, screenLock: SettingState): SettingState {
        if (screenLock == SettingState.OFF) return SettingState.ON
        if (screenOffMs == null) return SettingState.UNKNOWN
        if (neverSleeps(screenOffMs)) return SettingState.ON
        val total = screenOffMs + (lockAfterMs ?: DEFAULT_LOCK_AFTER_MS)
        return when {
            total > MAX_LOCK_DELAY_MS -> SettingState.ON
            screenLock == SettingState.UNKNOWN -> SettingState.UNKNOWN
            else -> SettingState.OFF
        }
    }

    private fun detail(context: Context, screenOff: Int?, lockAfter: Long?, screenLock: SettingState): String? {
        if (screenLock == SettingState.OFF) return context.getString(R.string.detail_lock_never)
        if (screenOff == null) return null
        if (neverSleeps(screenOff)) return context.getString(R.string.detail_timeout_never)
        val sleep = duration(context, screenOff.toLong())
        return when {
            lockAfter == null ->
                context.getString(R.string.detail_lock_delay_default, sleep, duration(context, DEFAULT_LOCK_AFTER_MS))
            lockAfter <= 0 -> context.getString(R.string.detail_lock_delay_immediate, sleep)
            else -> context.getString(R.string.detail_lock_delay_after, sleep, duration(context, lockAfter))
        }
    }

    override fun guideIntents(context: Context): List<Intent> {
        val screenOff = screenOff(context)
        val screenOffTooLong = screenOff == null || neverSleeps(screenOff) || screenOff > MAX_LOCK_DELAY_MS
        val display = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        val security = Intent(Settings.ACTION_SECURITY_SETTINGS)
        val lockDelayFirst = ScreenLockSetting.readScreenLock(context) != SettingState.OFF && !screenOffTooLong
        return if (lockDelayFirst) listOf(security, display) else listOf(display, security)
    }

    private fun neverSleeps(screenOffMs: Int) = screenOffMs <= 0 || screenOffMs == Int.MAX_VALUE

    private fun duration(context: Context, ms: Long): String {
        val seconds = ms / 1000
        return if (seconds < 60) {
            context.getString(R.string.duration_seconds, seconds.toInt())
        } else {
            context.getString(R.string.duration_minutes, (seconds / 60).toInt())
        }
    }

    private fun screenOff(context: Context): Int? = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    } catch (_: Settings.SettingNotFoundException) {
        null
    }

    private fun lockAfter(context: Context): Long? = try {
        Settings.Secure.getLong(context.contentResolver, HiddenKeys.LOCK_SCREEN_LOCK_AFTER_TIMEOUT)
    } catch (_: Settings.SettingNotFoundException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
