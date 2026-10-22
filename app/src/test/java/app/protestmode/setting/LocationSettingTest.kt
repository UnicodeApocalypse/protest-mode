package app.protestmode.setting

import android.location.LocationManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LocationSettingTest {
    @Test
    @Config(sdk = [28, 33])
    fun readsLocationManagerFromApi28() {
        val manager = context.getSystemService(LocationManager::class.java)

        shadowOf(manager).setLocationEnabled(true)
        assertEquals(SettingState.ON, LocationSetting.state(context))

        shadowOf(manager).setLocationEnabled(false)
        assertEquals(SettingState.OFF, LocationSetting.state(context))
    }

    @Suppress("DEPRECATION")
    @Test
    @Config(sdk = [23, 27])
    fun readsLocationModeBeforeApi28() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
        )
        assertEquals(SettingState.ON, LocationSetting.state(context))

        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF
        )
        assertEquals(SettingState.OFF, LocationSetting.state(context))
    }

    @Test
    fun guidesToLocationScreen() {
        assertTrue(LocationSetting.isAvailable(context))
        assertEquals(listOf(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LocationSetting.guideActions())
    }
}
