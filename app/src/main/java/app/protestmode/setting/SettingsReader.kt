package app.protestmode.setting

import android.content.ContentResolver
import android.provider.Settings

internal object SettingsReader {
    fun global(resolver: ContentResolver, key: String): Int? = read { Settings.Global.getInt(resolver, key) }

    fun secure(resolver: ContentResolver, key: String): Int? = read { Settings.Secure.getInt(resolver, key) }

    fun toState(value: Int?): SettingState = when (value) {
        null -> SettingState.UNKNOWN
        0 -> SettingState.OFF
        else -> SettingState.ON
    }

    private inline fun read(block: () -> Int): Int? = try {
        block()
    } catch (_: Settings.SettingNotFoundException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
