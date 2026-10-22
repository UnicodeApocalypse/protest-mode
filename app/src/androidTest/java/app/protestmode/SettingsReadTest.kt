package app.protestmode

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.setting.AirplaneSetting
import app.protestmode.setting.LocationSetting
import app.protestmode.setting.NfcSetting
import app.protestmode.setting.Probe
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingState
import app.protestmode.setting.UwbSetting
import app.protestmode.setting.WifiSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsReadTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pm = context.packageManager

    @Test
    fun everySettingReadsWithoutSecurityException() {
        SettingCatalog.all.forEach { setting ->
            setting.isAvailable(context)
            val state = setting.read(Probe(context)).state
            assertTrue(setting.id.name, state in SettingState.entries)
        }
    }

    @Test
    fun readableSettingsReturnAKnownState() {
        listOf(AirplaneSetting, LocationSetting).forEach {
            assertNotEquals(it.id.name, SettingState.UNKNOWN, it.read(Probe(context)).state)
        }
        if (WifiSetting.isAvailable(
                context
            )
        ) {
            assertNotEquals(SettingState.UNKNOWN, WifiSetting.read(Probe(context)).state)
        }
    }

    @Test
    fun availabilityMatchesHardware() {
        assertEquals(pm.hasSystemFeature(PackageManager.FEATURE_NFC), NfcSetting.isAvailable(context))
        val hasUwb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && pm.hasSystemFeature("android.hardware.uwb")
        assertEquals(hasUwb, UwbSetting.isAvailable(context))
    }

    @Test
    fun printsCompatibilityReport() {
        val start = SystemClock.elapsedRealtime()
        val probe = Probe(context)
        SettingCatalog.all.filter { it.isAvailable(context) }.forEach { it.read(probe) }
        Log.i(
            "ProtestModeCompat",
            "API ${Build.VERSION.SDK_INT} full read in ${SystemClock.elapsedRealtime() - start} ms"
        )
        SettingCatalog.all.forEach { setting ->
            val available = setting.isAvailable(context)
            val state = if (available) setting.read(Probe(context)).state.name else "-"
            Log.i(
                "ProtestModeCompat",
                "API ${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${setting.id} available=$available state=$state"
            )
        }
    }
}
