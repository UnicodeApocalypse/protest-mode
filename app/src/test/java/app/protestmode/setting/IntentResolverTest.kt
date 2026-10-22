package app.protestmode.setting

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class IntentResolverTest {
    private fun register(action: String) {
        val component = ComponentName("com.android.settings", action)
        val shadow = shadowOf(context.packageManager)
        shadow.addActivityIfNotPresent(component)
        shadow.addIntentFilterForActivity(
            component,
            IntentFilter(action).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )
    }

    @Test
    fun picksFirstResolvableIntent() {
        register(Settings.ACTION_WIRELESS_SETTINGS)
        register(Settings.ACTION_WIFI_SETTINGS)

        val intent =
            IntentResolver.resolve(
                context.packageManager,
                listOf(
                    Intent("com.example.MISSING"),
                    Intent(Settings.ACTION_WIFI_SETTINGS),
                    Intent(Settings.ACTION_WIRELESS_SETTINGS)
                )
            )

        assertEquals(Settings.ACTION_WIFI_SETTINGS, intent.action)
    }

    @Test
    fun fallsBackToMainSettings() {
        val intent = IntentResolver.resolve(context.packageManager, listOf(Intent("com.example.MISSING")))

        assertEquals(Settings.ACTION_SETTINGS, intent.action)
    }

    @Test
    fun everySettingEndsOnAResolvableScreen() {
        SettingCatalog.all.forEach { setting ->
            val intent = IntentResolver.resolve(context.packageManager, setting.guideIntents(context))
            assertEquals(setting.id.name, Settings.ACTION_SETTINGS, intent.action)
        }
    }
}
