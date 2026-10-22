package app.protestmode.setting

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class HotspotAndUwbSettingTest {
    @Test
    fun hotspotReadingNeverThrows() {
        assertTrue(HotspotSetting.state(context) in SettingState.entries)
    }

    @Test
    fun hotspotFallsBackToWirelessScreen() {
        assertEquals(Settings.ACTION_WIRELESS_SETTINGS, HotspotSetting.guideActions().last())
    }

    @Test
    fun uwbStateIsAlwaysUnknown() {
        assertEquals(SettingState.UNKNOWN, UwbSetting.state(context))
    }

    @Test
    @Config(sdk = [33])
    fun uwbAvailabilityFollowsHardware() {
        setFeature("android.hardware.uwb", true)
        assertTrue(UwbSetting.isAvailable(context))

        setFeature("android.hardware.uwb", false)
        assertFalse(UwbSetting.isAvailable(context))
    }

    @Test
    @Config(sdk = [30])
    fun uwbIsHiddenBeforeApi31() {
        setFeature("android.hardware.uwb", true)
        assertFalse(UwbSetting.isAvailable(context))
    }
}
