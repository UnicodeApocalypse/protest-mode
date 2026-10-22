package app.protestmode.setting

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows.shadowOf

internal val context: Context get() = ApplicationProvider.getApplicationContext()

internal fun setFeature(name: String, supported: Boolean = true) {
    shadowOf(context.packageManager).setSystemFeature(name, supported)
}

internal fun Setting.guideActions(): List<String?> = guideIntents(context).map(Intent::getAction)

internal fun Setting.state(context: Context): SettingState = read(Probe(context)).state

internal fun Setting.detail(context: Context): String? = read(Probe(context)).detail
