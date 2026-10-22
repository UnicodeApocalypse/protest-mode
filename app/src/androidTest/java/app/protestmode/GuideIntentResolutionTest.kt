package app.protestmode

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.setting.IntentResolver
import app.protestmode.setting.SettingCatalog
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuideIntentResolutionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pm = context.packageManager

    @Test
    fun mainSettingsScreenResolves() {
        assertNotNull(pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY))
    }

    @Test
    fun everySettingResolvesToAnActivity() {
        SettingCatalog.all.filter { it.isAvailable(context) }.forEach { setting ->
            val intent = IntentResolver.resolve(pm, setting.guideIntents(context))
            assertNotNull(setting.id.name, pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY))
            val first = setting.guideIntents(context).firstOrNull {
                pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) !=
                    null
            }
            Log.i(
                "ProtestModeCompat",
                "API ${Build.VERSION.SDK_INT} ${setting.id} guide=${first?.action ?: "fallback ACTION_SETTINGS"}"
            )
        }
    }
}
