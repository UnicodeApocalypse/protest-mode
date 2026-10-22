package app.protestmode.setting

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

object IntentResolver {
    fun resolve(packageManager: PackageManager, candidates: List<Intent>): Intent =
        candidates.firstOrNull { packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null }
            ?: Intent(Settings.ACTION_SETTINGS)
}
