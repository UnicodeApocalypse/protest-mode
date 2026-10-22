package app.protestmode.setting

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.protestmode.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AirplaneSettingTest {
    @Test
    fun readsGlobalSetting() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        assertEquals(SettingState.ON, AirplaneSetting.state(context))

        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        assertEquals(SettingState.OFF, AirplaneSetting.state(context))
    }

    @Test
    fun isAlwaysAvailableAndNeverAutomatic() {
        assertTrue(AirplaneSetting.isAvailable(context))
        assertFalse(AirplaneSetting.tryProtect(context))
    }

    @Test
    fun guidesToAirplaneScreenFirst() {
        assertEquals(
            listOf(Settings.ACTION_AIRPLANE_MODE_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS),
            AirplaneSetting.guideActions()
        )
    }

    @Test
    fun simpleToggleNeedsNoInstruction() {
        assertNull(AirplaneSetting.instructionRes)
    }

    @Test
    fun detailMentionsCellTowerLocation() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
        assertEquals(context.getString(R.string.detail_airplane_off), AirplaneSetting.detail(context))

        Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
        assertEquals(context.getString(R.string.detail_airplane_on), AirplaneSetting.detail(context))
    }
}
