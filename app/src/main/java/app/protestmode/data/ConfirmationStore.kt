package app.protestmode.data

import android.content.Context
import app.protestmode.setting.SettingId

class ConfirmationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): Map<SettingId, String> = prefs.all.mapNotNull { (name, value) ->
        val id = SettingId.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
        (value as? String)?.let { id to it }
    }.toMap()

    fun save(confirmations: Map<SettingId, String>) {
        val editor = prefs.edit().clear()
        confirmations.filterValues {
            it.isNotEmpty()
        }.forEach { (id, fingerprint) -> editor.putString(id.name, fingerprint) }
        editor.apply()
    }

    private companion object {
        const val FILE = "confirmations"
    }
}
