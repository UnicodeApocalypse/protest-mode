package app.protestmode.setting

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.os.ConfigurationCompat
import app.protestmode.R
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object SecurityPatchSetting : Setting {
    internal val MAX_AGE_MS = TimeUnit.DAYS.toMillis(183)

    override val id = SettingId.SECURITY_PATCH
    override val titleRes = R.string.setting_security_patch
    override val iconRes = R.drawable.ic_system_update
    override val instructionRes = R.string.instruction_security_patch
    override val onLabelRes = R.string.state_up_to_date
    override val offLabelRes = R.string.state_outdated

    override fun isAvailable(context: Context) = true

    override fun read(probe: Probe): Reading {
        val date = parse(Build.VERSION.SECURITY_PATCH) ?: return Reading(SettingState.UNKNOWN)
        // The app language may differ from the system one (per-app language on Android 13+).
        val locale = ConfigurationCompat.getLocales(probe.context.resources.configuration)[0] ?: Locale.getDefault()
        val formatted = DateFormat.getDateInstance(DateFormat.LONG, locale).format(date)
        return Reading(
            stateFor(date, System.currentTimeMillis()),
            probe.context.getString(R.string.detail_security_patch, formatted)
        )
    }

    internal fun stateFor(patch: String?, nowMs: Long): SettingState =
        parse(patch)?.let { stateFor(it, nowMs) } ?: SettingState.UNKNOWN

    private fun stateFor(patch: Date, nowMs: Long) = (nowMs - patch.time <= MAX_AGE_MS).toSettingState()

    override fun guideIntents(context: Context): List<Intent> =
        listOf(Intent(HiddenActions.SYSTEM_UPDATE), Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))

    private fun parse(patch: String?): Date? {
        if (patch.isNullOrBlank()) return null
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return try {
            format.parse(patch)
        } catch (_: ParseException) {
            null
        }
    }
}
